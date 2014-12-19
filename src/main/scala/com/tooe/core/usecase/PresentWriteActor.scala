package com.tooe.core.usecase

import com.toiserver.core.usecase.notification.domain.Sender
import com.toiserver.core.usecase.notification.domain.{Present => NotificationPresent, Location => NotificationLocation}
import com.toiserver.core.usecase.notification.message.{PresentSMS, ConfirmationEmail, PresentEmail}
import com.tooe.api.service.{Gateway, RouteContext, ExecutionContextProvider, SuccessfulResponse}
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.domain._
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.usecase.present.PresentDataActor
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.usecase.security.CredentialsDataActor
import com.tooe.core.usecase.urls.UrlsWriteActor
import com.tooe.core.util.{Lang, InfoMessageHelper, DateHelper}
import payment.PaymentDataActor
import scala.concurrent.Future
import user.UserDataActor
import wish.WishDataActor
import com.tooe.core.usecase.location.LocationDataActor
import org.codehaus.jackson.annotate.JsonProperty

object PresentWriteActor {
  final val Id = Actors.PresentWrite

  case class MakePresent(orderId: Payment.OrderId)
  case class DeleteUserPresent(presentId: PresentId, userId: UserId)
  case class ActivatePresent(request: ChangePresentStatusRequest, presentId: PresentId, ctx: RouteContext)
  case class CommentPresent(presentId: PresentId, request: CommentPresentRequest)
}

class PresentWriteActor extends AppActor with ExecutionContextProvider{

  lazy val paymentDataActor = lookup(PaymentDataActor.Id)
  lazy val presentDataActor = lookup(PresentDataActor.Id)
  lazy val wishDataActor = lookup(WishDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val userEventWriteActor = lookup(UserEventWriteActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val notificationActor = lookup(Actors.NotificationActor)
  lazy val credentialsDataActor = lookup(CredentialsDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)

  import PresentWriteActor._

  def receive = {
    case ActivatePresent(request, presentId, ctx) =>
      implicit val lang = ctx.lang
      val result = for {
        present <- getPresent(presentId)
        _ <- if (present.expiresAt.after(DateHelper.currentDate)) Future.successful()
             else InfoMessageHelper.throwAppExceptionById("present_is_expired")
        _ <- if (!present.isReceived) Future.successful()
             else InfoMessageHelper.throwAppExceptionById("present_is_received")
      } yield {
        presentDataActor ! PresentDataActor.ActivatePresent(present.id)
        paymentDataActor ! PaymentDataActor.ActivatePayment(present.orderId)
        SuccessfulResponse
      }
      result pipeTo sender

    case MakePresent(orderId) =>
      val paymentFtr = getPayment(orderId)
      val future = paymentFtr map createPresent(orderId) map savePresent
      future onSuccess PartialFunction(fulfillWish)
      future onSuccess PartialFunction(addGift)
      future onSuccess PartialFunction(createUserEvent)
      (future zip paymentFtr) onSuccess {
        case (present, payment) if !payment.recipient.isPrivate => createNews(present)
      }
      future onSuccess PartialFunction(incrementSalesCount)
      future onSuccess PartialFunction(incrementPresentsCount)
      future onSuccess PartialFunction(addUrlsForPresentMedia)
      future onSuccess PartialFunction(sendNotifications)
      future onFailure { case t => log.error(t, "") }

    case CommentPresent(presentId, request) =>
      presentDataActor ! PresentDataActor.CommentPresent(presentId, request.comment)
      sender ! SuccessfulResponse

    case DeleteUserPresent(presentId, userId) =>
      markAsRemoved(presentId, userId).map {
        case UpdateResult.Updated => SuccessfulResponse
        case _ => throw new ApplicationException(message = "Present has not been removed")
      }.pipeTo(sender)
  }

  def getPresent(presentId: PresentId) =
    (presentDataActor ? PresentDataActor.GetPresent(presentId)).mapTo[Present]

  def markAsRemoved(presentId: PresentId, userId: UserId) =
    presentDataActor.ask(PresentDataActor.MarkAsRemoved(presentId, userId)).mapTo[UpdateResult]

  def getPayment(orderId: Payment.OrderId): Future[Payment] =
    (paymentDataActor ? PaymentDataActor.GetPayment(orderId)).mapTo[Payment]

  def savePresent(present: Present): Present = {
    presentDataActor ! PresentDataActor.Save(present)
    present
  }

  def fulfillWish(present: Present): Unit = tryToFulfillWish(present) onSuccess {
    case Some(wish) => changeWishesCounters(wish)
  }

  def tryToFulfillWish(present: Present): Future[Option[Wish]] = present.userId map { userId =>
    (wishDataActor ? WishDataActor.Fulfill(userId, present.product.productId)).mapTo[Option[Wish]]
  } getOrElse Future.successful(None)

  def addGift(present: Present): Unit = userDataActor ! UserDataActor.AddGift(present.senderId, present.id)

  def changeWishesCounters(wish: Wish): Unit = {
    val userId = wish.userId
    updateStatisticActor ! UpdateStatisticActor.ChangeUsersWishesCounter(userId, -1)
    updateStatisticActor ! UpdateStatisticActor.ChangeUsersFulfilledWishesCounter(userId, 1)
  }

  def incrementPresentsCount(present: Present): Unit = {
    productDataActor ! ProductDataActor.IncreasePresentCounter(present.product.productId, +1)
    present.product.productTypeId match {
      case ProductTypeId.Product     => newProductGiven(present)
      case ProductTypeId.Certificate => newCertificateGiven(present)
      case _                         => //do nothing
    }
  }

  def newProductGiven(present: Present): Unit = {
    present.userId foreach { userId =>
      updateStatisticActor ! UpdateStatisticActor.ChangeUserNewPresentsCounter(userId, 1)
      updateStatisticActor ! UpdateStatisticActor.ChangeUserPresentsCounter(userId, 1)
    }
    updateStatisticActor ! UpdateStatisticActor.ChangeUserSentPresentsCounter(present.senderId, 1)
  }

  def newCertificateGiven(present: Present): Unit = {
    present.userId foreach { userId =>
      updateStatisticActor ! UpdateStatisticActor.ChangeUserNewPresentsCounter(userId, 1)
      updateStatisticActor ! UpdateStatisticActor.ChangeUserCertificatesCounter(userId, 1)
    }
    updateStatisticActor ! UpdateStatisticActor.ChangeUserSentCertificatesCounter(present.senderId, 1)
  }

  def incrementSalesCount(present: Present): Unit = {
    updateStatisticActor ! UpdateStatisticActor.ChangeSalesCounter(present.product.locationId, 1)
    updateStatisticActor ! UpdateStatisticActor.ChangeLocationPresentCounter(present.product.locationId, 1)
  }

  def createUserEvent(present: Present): Unit = userEventWriteActor ! UserEventWriteActor.NewPresentReceived(present)

  def createNews(present: Present): Unit =
    newsWriteActor ! NewsWriteActor.AddPresentNews(present)

  def addUrlsForPresentMedia(present: Present): Unit = present.product.media.map { media =>
    urlsWriteActor ! UrlsWriteActor.AddPresentUrl(present.id, media.url.url)
  }

  def sendNotifications(present: Present): Unit = {
    val userIds = (present.userId.toSet + present.senderId).toSeq
    for {
      usersMap <- userDataActor.ask( UserDataActor.GetUsers(userIds)).mapTo[Seq[User]].map(_.toMapId(_.id))
      credentialsMap <- credentialsDataActor.ask(CredentialsDataActor.GetCredentialsByUserIds(userIds)).mapTo[Seq[Credentials]].map(_.toMapId(_.userId))
      location <- locationDataActor.ask(LocationDataActor.GetLocation(present.product.locationId)).mapTo[Location]
      payment <- getPayment(present.orderId)
      sendNotificationsResult <- trySendingNotifications(payment, present, location)(usersMap)
    } {
      if (sendNotificationsResult) sendConfirmationEmail(present, location)(credentialsMap, usersMap)
      else log.warning("Present notifications haven't been sent neither via SMS nor via e-mail. OrderId={}", payment.orderId)
    }
  }

  def getRecipientEmail(orderId: Payment.OrderId): Future[String] = getPayment(orderId) map (_.recipient.email)

  def trySendingNotifications(payment: Payment, present: Present, location: Location)(usersMap: Map[UserId, User]): Future[Boolean] =
    sendEmailNotification(payment, present, location)(usersMap) zip sendSmsNotification(payment, present, location)(usersMap) map {
      case (r1, r2) => r1 || r2
    }

  def sendSmsNotification(payment: Payment, present: Present, location: Location)(usersMap: Map[UserId, User]): Future[Boolean] = {
    val msgOpt = for {
      code <- payment.recipient.getPhoneCode
      number <- payment.recipient.getPhone
    } yield PresentMailHelper.presentSms(phoneCode = code, phone = number, present, location)(usersMap)

    msgOpt map { msg =>
      (notificationActor ? msg)(timeout.duration * 2).mapTo[Boolean]
    } getOrElse Future.successful(false)
  }

  def sendEmailNotification(payment: Payment, present: Present, location: Location)(usersMap: Map[UserId, User]): Future[Boolean] = {
    payment.recipient.getEmail map { email =>
      val msg = PresentMailHelper.presentEmail(email = email, present, location)(usersMap)
      (notificationActor ? msg)(timeout.duration * 2).mapTo[Boolean]
    } getOrElse Future.successful(false)
  }

  def sendConfirmationEmail(present: Present, location: Location)(credentialsMap: Map[UserId, Credentials], usersMap: Map[UserId, User]) = {
    val msg = PresentMailHelper.confirmationEmail(present, location)(credentialsMap, usersMap)
    notificationActor ! msg
  }

  def createPresent(orderId: Payment.OrderId)(payment: Payment) = {
    import DateHelper._
    val p = payment.productSnapshot
    val now = currentDate
    val anonymousRecipient =
      if (payment.recipient.getRecipientId.nonEmpty) None
      else Some(AnonymousRecipient(
        phone = payment.recipient.getPhoneShort,
        email = payment.recipient.getEmail
      ))
    val result = Present(
      id = PresentId(),
      code = PresentCode(orderId.toString()),
      userId = payment.recipient.getRecipientId,
      anonymousRecipient = anonymousRecipient,
      senderId = payment.getUserId,
      hideSender = payment.recipient.hideSender,
      message = p.getMessage,
      createdAt = now,
      expiresAt = now addDays p.validity,
      product = PresentProduct(
        companyId = p.getCompanyId,
        locationId = p.getLocationId,
        productId = p.getProductId,
        productName = p.name,
        productTypeId = p.getProductTypeId,
        description = p.descr,
        media = p.getPictureUrl.map (p => PresentProductMedia(
          url = MediaObject(MediaObjectId(p)),
          description = None //TODO
        ))
      ),
      orderId = orderId,
      adminComment = None
    )
    log.info(s"Creating present: $result")
    result
  }
}

case class ChangePresentStatusRequest(status: String, gateway: Option[Gateway]) extends UnmarshallerEntity

case class CommentPresentRequest(@JsonProperty("admcomment") comment: String) extends UnmarshallerEntity

object PresentMailHelper {

  def presentSms(phoneCode: String, phone: String, present: Present, location: Location)(usersMap: Map[UserId, User]) = {
    val fullName = recipientFullName(present)(usersMap)
    new PresentSMS(phone, phoneCode, fullName, notificationSenderByActor(present, usersMap), notificationPresent(present), notificationLocation(location))
  }

  def confirmationEmail(present: Present, location: Location)(credentialsMap: Map[UserId, Credentials], usersMap: Map[UserId, User]) = {
    val recipientOpt = present.userId.map(id => usersMap(id))
    val actorCredentials = credentialsMap(present.senderId)
    val actor = usersMap(present.senderId)
    new ConfirmationEmail(actorCredentials.userName, actor.fullName, recipientOpt.map(_.fullName).getOrElse(""), present.product.productName, notificationLocation(location))
  }

  def presentEmail(email: String, present: Present, location: Location)(usersMap: Map[UserId, User]) =
    new PresentEmail(
      email,
      recipientFullName(present)(usersMap),
      notificationSenderByActor(present, usersMap),
      notificationPresent(present),
      notificationLocation(location)
    )

  def recipientFullName(present: Present)(usersMap: Map[UserId, User]) = {
    val recipientOpt = present.userId.map(id => usersMap(id))
    recipientOpt map (_.fullName) getOrElse ""
  }

  def notificationSenderByActor(present: Present,usersMap: Map[UserId, User]): Sender = {
    present.actorId.map(id => usersMap(id)).map(actor => sender(actor)).getOrElse(new Sender("", "", ""))
  }

  def sender(user: User): Sender = new Sender(user.name, user.lastName, user.gender.id)
  def notificationPresent(present: Present): NotificationPresent = new NotificationPresent(present.id.id.toString, present.product.productName, present.code.value, present.expiresAt)
  def notificationLocation(location: Location): NotificationLocation = new NotificationLocation(
    location.name.localized(Lang.ru) getOrElse "",   // TODO  #3266
    location.contact.address.country,
    location.contact.address.regionName,
    location.contact.address.street,
    location.mainPhone.map(_.number) getOrElse "",
    location.mainPhone.map(_.countryCode) getOrElse ""
  )
}