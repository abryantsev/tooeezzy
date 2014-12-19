package com.tooe.core.usecase.payment

import akka.actor.{ActorRef, Actor}
import akka.pattern.{ask, pipe}
import com.tooe.core.util.ActorHelper
import com.tooe.core.usecase.{InfoMessageActor, PresentWriteActor, location}
import location.LocationDataActor
import com.tooe.core.application.Actors
import java.sql.Timestamp
import com.tooe.core.db.mysql.domain
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.application.AppActors
import com.tooe.core.domain._
import com.tooe.core.db.mysql.services.StateChangeResult
import com.tooe.core.util.Lang
import com.tooe.core.usecase.user.UserDataActor
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.UserContact
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.util.DateHelper
import com.tooe.core.db.mysql.services.StateChangeResult._
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.usecase.region.RegionDataActor
import java.util.Date
import com.tooe.core.db.mongo.domain.Region
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.db.mongo.domain.User
import com.tooe.api.service.RouteContext
import com.tooe.core.db.mongo.domain.Product
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.db.mysql.domain.ProductSnapshot

object PaymentWorkflowActor {
  final val Id = Actors.PaymentWorkflow

  case class InitPayment(request: PaymentRequest, userId: UserId, routeContext: RouteContext)
  case class CheckPayment(orderId: Payment.OrderId)
  case class RejectPayment(orderId: Payment.OrderId)
  case class ConfirmPayment(orderId: Payment.OrderId, transactionId: String, fullPrice: Option[BigDecimal] = None)

  sealed trait CheckResult
  object CheckResult {
    case object CheckPassed extends CheckResult
    case object CheckFailed extends CheckResult
  }
}

class PaymentWorkflowActor
(
  productDataActor: ActorRef,
  locationDataActor: ActorRef,
  paymentDataActor: ActorRef
  )
  extends Actor with ActorHelper with AppActors with DefaultTimeout {

  lazy val presentWriteActor: ActorRef = lookup(Actors.PresentWrite)
  lazy val userDataActor: ActorRef = lookup(UserDataActor.Id)
  lazy val regionDataActor: ActorRef = lookup(RegionDataActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)

  import PaymentWorkflowActor._
  import context.dispatcher

  def receive = {
    case InitPayment(request, userId, routeContext) =>
      val future = for {
        product <- getProduct(request.productId) flatMap checkAvailability
        _ <- checkPrice(request.amount, product.priceWithDiscount(getCurrentDate()))(routeContext.lang)
        location <- locationDataActor.ask(LocationDataActor.GetLocation(product.location.id)).mapTo[Location]
        rc <- recipientContacts(request.recipient)
        payment = createPayment(request, userId, product, location, rc)(routeContext.lang)
        savedPayment <- paymentDataActor.ask(PaymentDataActor.SavePayment(payment)).mapTo[Payment]
      } yield savedPayment
      future pipeTo sender

    case CheckPayment(orderId) =>
      import CheckResult._
      getPayment(orderId) flatMap { payment =>
        if (payment.isOpen) prolongPayment(payment) map { updated => if (updated) CheckPassed else CheckFailed }
        else if (payment.isSucceeded) Future successful CheckPassed
        else Future successful CheckFailed
      } pipeTo sender

    case RejectPayment(orderId) =>
      rejectPayment(orderId) pipeTo sender onSuccess logStatusChangingPF(orderId, "Rejected")

    case ConfirmPayment(orderId, transactionId, fullPriceOpt) =>
      val decrementProductFtr = decrementProductAvailability(orderId)

      val confirmPaymentFtr = decrementProductFtr flatMap { _ => confirmPayment(orderId, transactionId, fullPriceOpt) }
      confirmPaymentFtr pipeTo sender
      confirmPaymentFtr onSuccess logStatusChangingPF(orderId, "Confirmed")
      confirmPaymentFtr onSuccess {
        case JustChanged => makePresent(orderId) //TODO test that it reacts on JustChanged only
      }
      (decrementProductFtr zip confirmPaymentFtr) onSuccess {
        case (Some(productId), AlreadyChanged) => incrementProductAvailability(productId)
      }
  }

  def checkPrice(amount: Amount, price: Price)(implicit lang: Lang): Future[_] =
    if (amount.value == price.value) Future successful ()
    else getFailure("payment_wrong_price")(lang)

  def getFailure(id: String)(implicit lang: Lang): Future[_] =
    infoMessageActor ? InfoMessageActor.GetFailure(id, lang)

  def makePresent(orderId: Payment.OrderId): Unit = presentWriteActor ! PresentWriteActor.MakePresent(orderId)

  def rejectPayment(orderId: Payment.OrderId): Future[StateChangeResult] = //TODO test
    (paymentDataActor ? PaymentDataActor.RejectPayment(orderId)).mapTo[StateChangeResult]

  def confirmPayment(orderId: Payment.OrderId, transactionId: String, fullPriceOpt: Option[BigDecimal]): Future[StateChangeResult] = //TODO test
    (paymentDataActor ? PaymentDataActor.ConfirmPayment(orderId, transactionId, fullPriceOpt)).mapTo[StateChangeResult]

  def getPayment(orderId: Payment.OrderId): Future[Payment] =
    (paymentDataActor ? PaymentDataActor.GetPayment(orderId)).mapTo[Payment]

  def prolongPayment(payment: Payment): Future[Boolean] = {
    import DateHelper._
    val newExpireDate = payment.getExpireDate addMillis (payment.getExpireDate.getTime - payment.getDate.getTime)
    (paymentDataActor ? PaymentDataActor.UpdateExpireDate(payment.orderId, newExpireDate)).mapTo[Int] map { _ == 1 }
  }

  def decrementProductAvailability(orderId: Payment.OrderId): Future[Option[ProductId]] = //TODO test
    getPayment(orderId) flatMap { payment => decrementProductAvailability(payment.productSnapshot.getProductId) }

  def decrementProductAvailability(productId: ProductId): Future[Option[ProductId]] =
    (productDataActor ? ProductDataActor.ChangeAvailability(productId, -1)).mapTo[UpdateResult] map {
      case UpdateResult.Updated => Some(productId)
      case _                    => None
    }

  def incrementProductAvailability(productId: ProductId): Unit = //TODO test
    productDataActor ! ProductDataActor.ChangeAvailability(productId, 1)

  def getProduct(id: ProductId): Future[Product] = (productDataActor ? ProductDataActor.GetProduct(id)).mapTo[Product]

  def checkAvailability(product: Product): Future[Product] = //TODO test
    countOpenPayments(product.id) map (openPaymentCount => {
        val isAvailable = product.availabilityCount map (_ - openPaymentCount) map (_ > 0) getOrElse true
        if (isAvailable) product
        else throw ApplicationException(message = "Product is not available")
      }
    )

  def countOpenPayments(id: ProductId): Future[Long] = //TODO test
    (paymentDataActor ? PaymentDataActor.CountOpenPayments(id)).mapTo[Long]

  def logStatusChangingPF(orderId: Payment.OrderId, status: String): PartialFunction[StateChangeResult, Unit] = {
    def orderIdStr = "Payment(orderId = " + orderId.toString() + ")"
    import StateChangeResult._
    {
      case JustChanged => log.info(orderIdStr+" has changed to "+status)
      case AlreadyChanged => log.warning(orderIdStr+" can't be changed to "+status+", it's finished already")
      case PaymentNotFound => log.error(orderIdStr+" can't be changed to "+status+", it's not found")
    }
  }

  implicit def stringCutter(str: String) = new {
    private val suffix = "..."
    def cutIfLongerThan(limit: Int) = if (str.length > limit) str.take(limit-suffix.length) + suffix else str
  }

  private def createPayment(params: PaymentRequest, userId: UserId, product: Product, location: Location, rc: RecipientContacts)
    (implicit lang: Lang): Payment =
  {
    log.info(s"RecipientContacts: $rc")
    import DateHelper._
    val now = getCurrentDate()
    val payment = Payment(
      userId = userId.id.toString,
      amount = params.amount.value.bigDecimal,
      currencyId = params.amount.currency,
      expireDate = new Timestamp(now.addMinutes(getPaymentExpiresInMins).getTime)
    )
    payment.date = new Timestamp(now.getTime)
    payment.paymentSystem = com.tooe.core.db.mysql.domain.PaymentSystem(
      payment,
      system = params.paymentSystem.system,
      subSystem = params.paymentSystem.subsystem.orNull
    )
    payment.productSnapshot = ProductSnapshot(
      payment,
      productId = params.productId.id.toString,
      product_type = product.productTypeId.id,
      name = product.name.localized.map(_ cutIfLongerThan 100) getOrElse "",
      descr = product.description.localized.map(_ cutIfLongerThan 200) getOrElse "",
      price = product.priceWithDiscount(now).value.bigDecimal,
      currency = product.price.currency.id,
      loc_id = location.id.id.toString,
      loc_name = location.name.localized.map(_ cutIfLongerThan 100) getOrElse "",
      loc_city = location.contact.address.regionName,
      loc_country = location.contact.address.country cutIfLongerThan 20,
      loc_street = location.contact.address.street cutIfLongerThan 200,
      validity = product.validityInDays,
      companyId = location.companyId.id.toString,
      picture_url = product.productMedia.headOption.map(_.media.url.id) getOrElse null,
      present_msg = params.msg cutIfLongerThan 3000,
      article = product.article.map(_ cutIfLongerThan 50).orNull
    )
    payment.recipient = domain.Recipient(
      payment,
      recipientId = rc.id.map(_.id.toString).orNull,
      //TODO truncate data below due to constraints in the db
      email = rc.email.map(_.take(50)).orNull,
      phone = rc.phone.map(_.take(20)).orNull,
      countryId = rc.countryId.map(_.id.take(2)).orNull,
      phoneCode = rc.phoneCode.map(_.take(10)).orNull,
      showActor = params.hideActor map (!_) getOrElse true,
      isPrivate = params.isPrivate getOrElse false
    )
    log.info(s"Creating payment: " + payment)
    payment
  }

  def getCurrentDate(): Date = DateHelper.currentDate

  def getPaymentExpiresInMins: Int = settings.Payment.PaymentExpiresInMin

  case class RecipientContacts(id: Option[UserId], email: Option[String], phone: Option[String], countryId: Option[CountryId], phoneCode: Option[String])

  object RecipientContacts {
    def apply(r: Recipient): RecipientContacts  =
      RecipientContacts(
        id = r.id,
        email = r.email,
        phone = r.phone,
        countryId = r.country map (_.id),
        phoneCode = r.country map (_.phone)
      )

    def apply(id: UserId, contact: UserContact, countryId: CountryId): RecipientContacts = {
      val phone = contact.phones.main
      RecipientContacts(
        id = Some(id),
        email = Some(contact.email),
        phone = phone.map(_.number),
        countryId = Some(countryId),
        phoneCode = phone.map(_.countryCode)
      )
    }
  }

  def recipientContacts(r: Recipient): Future[RecipientContacts] = {

    def byUserId(id: UserId) =
      for {
        contact <- getUserContacts(id)
        countryId <- getUserCountryId(contact.address.regionId)
      } yield RecipientContacts(id, contact, countryId)

    def userIdByEmail = r.email map findUserIdByEmail

    def userIdByPhone =
      for {
        code <- r.country.map(_.phone)
        number <- r.phone
      } yield findUserIdByPhone(code, number)

    def byRecipientContacts =
      for {
        userId <- userIdByEmail orElse userIdByPhone getOrElse Future.successful(None)
        result <- userId map byUserId getOrElse Future.successful(RecipientContacts(r).copy(id = userId))
      } yield result

    r.id map byUserId getOrElse byRecipientContacts
  }

  def findUserIdByEmail(email: String): Future[Option[UserId]] =
    (userDataActor ? UserDataActor.FindUserIdByEmail(email)).mapTo[Option[UserId]]

  def findUserIdByPhone(code: String, number: String): Future[Option[UserId]] =
    (userDataActor ? UserDataActor.FindUserIdByPhone(code = code, number = number)).mapTo[Option[UserId]]

  def getUserContacts(id: UserId): Future[UserContact] = getUser(id) map (_.contact)

  def getUser(id: UserId): Future[User] = (userDataActor ? UserDataActor.GetUser(id)).mapTo[User]

  def getUserCountryId(id: RegionId): Future[CountryId] = getRegion(id) map (_.countryId)

  def getRegion(id: RegionId): Future[Region] = (regionDataActor ? RegionDataActor.GetRegion(id)).mapTo[Region]
}