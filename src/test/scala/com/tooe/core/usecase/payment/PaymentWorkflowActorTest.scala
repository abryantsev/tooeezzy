package com.tooe.core.usecase.payment

import com.tooe.core.usecase.ActorTestSpecification
import akka.actor._
import akka.testkit.{TestProbe, TestActorRef, TestKit}
import scala.util.Random
import com.tooe.core.db.mongo.domain._
import com.tooe.core.util.{DateHelper, TestDateGenerator, HashHelper, Lang}
import com.tooe.core.db.mysql.services.StateChangeResult.{AlreadyChanged, JustChanged}
import org.bson.types.ObjectId
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.usecase.location.LocationDataActor
import scala.concurrent.{Promise, Future}
import com.tooe.core.usecase.product.ProductDataActor
import java.sql.Timestamp
import com.tooe.core.db.mysql.services.StateChangeResult
import com.tooe.core.domain._
import com.tooe.api.service.RouteContext
import com.tooe.core.exceptions.ApplicationException
import java.util.Date
import com.tooe.core.service.PaymentFixture
import com.tooe.core.usecase.user.UserDataActor

class PaymentWorkflowActorTest extends ActorTestSpecification {

  import PaymentWorkflowActor._

  "PaymentWorkflowActor" should {
    "create payment entity on InitPayment" >> {

      val f = new PaymentWorkflowActorFixture() {
        def presentActorRef: ActorRef = null

        val productPrice = BigDecimal("231.35")
        val discountPercent = 20
        val endPrice = productPrice * (100 - discountPercent) / 100

        val productFixture = new ProductFixture(price = productPrice, discountPercent = discountPercent)
        val productPictureUrl = productFixture.pictureUrl
        val product = productFixture.product

        val Request = PaymentRequest(
          productId = product.id,
          recipient = new Recipient(email = Some("a.b@d")),
          msg = "some-present-message",
          amount = Amount(endPrice, "RUR"),
          paymentSystem = PaymentSystem(system = ""),
          responseType = ResponseType.JSON,
          pageViewType = None
        )

        def productDataActorRef = TestActorRef[Actor](Props(new Actor {
          def receive = {
            case ProductDataActor.GetProduct(productId) =>
              productId === Request.productId
              sender ! product
          }
        }))
        def locationActorRef = TestActorRef[Actor](Props(new Actor {
          def receive = {
            case LocationDataActor.GetLocation(id) =>
              product.location.id === id
              sender ! Location(
                companyId = CompanyId(),
                locationsChainId = None,
                name = Map("orig"->"name"),
                contact = LocationContact(
                  address = LocationAddress(
                    coordinates = Coordinates(),
                    regionId = RegionId(null),
                    countryId = CountryId("ro"),
                    regionName = "",
                    country = "",
                    street = ""
                  )
                ),
                lifecycleStatusId = None
              )
          }
        }))
        def paymentDataActorRef = TestActorRef[Actor](Props(new Actor {
          def receive = {
            case PaymentDataActor.SavePayment(p) => sender ! p
          }
        }))

        val availabilityChecked = Promise[Boolean]()

        override def checkProductAvailabilityStub(p: Product) = {
          availabilityChecked success true
          p === product
          Future successful product
        }

        val priceHasBeenChecked = Promise[(Amount, Price)]()

        override def checkPriceStub(amount: Amount, price: Price): Future[_] = {
          priceHasBeenChecked success (amount, price)
          Future successful ()
        }
      }

      val probe = f.paymentWorkflowActor.probeTells(
        PaymentWorkflowActor.InitPayment(f.Request, UserId(new ObjectId()), RouteContext("v01", Lang.ru))
      )

      val payment = probe.expectMsgType[Payment]

      f.availabilityChecked.awaitResult === true
      val prices = f.priceHasBeenChecked.awaitResult
      prices._1.value === f.endPrice
      prices._2.value === f.endPrice

      payment !== null
      payment.date === f.paymentDate
      payment.expireDate === f.expiresAt
      
      val product = payment.productSnapshot
      product != null
      product.present_msg === f.Request.msg
      product.validity === f.product.validityInDays
      product.picture_url === f.productPictureUrl
      product.article === f.product.article.get
      BigDecimal(product.price) === f.endPrice
      
      //TODO check payment result
    }
    "prevent selling presents for the price less or more than in the db" >> {
      val f = new PaymentWorkflowActorCheckAvailabilityFixture()
      implicit val lang = Lang.ru
      f.paymentWorkflowActor.checkPrice(Amount(BigDecimal("299.99"), "RUR"), Price(BigDecimal("99.99"), CurrencyId.RUR)).value.get.failed.get === f.MsgException
      f.paymentWorkflowActor.checkPrice(Amount(BigDecimal("99.99"), "RUR"), Price(BigDecimal("99.99"), CurrencyId.RUR)).value.get.isSuccess === true
      f.paymentWorkflowActor.checkPrice(Amount(BigDecimal("99.99"), "RUR"), Price(BigDecimal("299.99"), CurrencyId.RUR)).value.get.failed.get === f.MsgException
    }
  }

  "PaymentWorkflowActor CheckPayment" should {
    "pass when payment exists and isOpen and has been prolonged" >> {
      val f = new PaymentWorkflowActorFixture.WithGetPaymentMock(prolongPaymentResult = true)
      f.paymentWorkflowActor probeTells CheckPayment(f.OrderId) expectMsg CheckResult.CheckPassed
      f.prolongProbe.expectMsgType[Payment]
      success
    }
    "pass when payment exists and isOpen and has NOT been prolonged" >> {
      val f = new PaymentWorkflowActorFixture.WithGetPaymentMock(prolongPaymentResult = false)
      f.paymentWorkflowActor probeTells CheckPayment(f.OrderId) expectMsg CheckResult.CheckFailed
      f.prolongProbe.expectMsgType[Payment]
      success
    }
    "return CheckPassed when check request comes and the payment committed" >> {
      val f = new PaymentWorkflowActorFixture.WithGetPaymentMock() {
        override def paymentFactory = {
          val p = new Payment()
          p.transactionId = "not-null-id"
          p
        }
      }
      f.paymentWorkflowActor probeTells CheckPayment(f.OrderId) expectMsg CheckResult.CheckPassed
      f.prolongProbe expectNoMsg ()
      success
    }
    "return CheckFailed when check request comes and the payment rejected" >> {
      val f = new PaymentWorkflowActorFixture.WithGetPaymentMock() {
        override def paymentFactory = {
          val p = new Payment()
          p.rejected = true
          p
        }
      }
      f.paymentWorkflowActor probeTells CheckPayment(f.OrderId) expectMsg CheckResult.CheckFailed
      f.prolongProbe expectNoMsg ()
      success
    }
    "return CheckFailed when check request comes and the payment deleted" >> {
      val f = new PaymentWorkflowActorFixture.WithGetPaymentMock() {
        override def paymentFactory = {
          val p = new Payment()
          p.deleted = true
          p
        }
      }
      f.paymentWorkflowActor probeTells CheckPayment(f.OrderId) expectMsg CheckResult.CheckFailed
      f.prolongProbe expectNoMsg ()
      success
    }
  }

  "PaymentWorkflowActor.prolongPayment" should {
    val f = new PaymentWorkflowActorProlongPaymentFixture()
    import f._
    "extend expireDate in two times" >> {
      paymentWorkflowActor.prolongPayment(payment)
      val timeout = payment.expireDate.getTime - payment.date.getTime
      val expected = payment.expireDate.getTime + timeout

      val msg = probe.expectMsgType[PaymentDataActor.UpdateExpireDate]
      msg.expireDate.getTime must beGreaterThan (payment.expireDate.getTime)
      msg.expireDate.getTime === expected
    }
    "return false if expireDate update has failed" >> {
      val resultFtr = paymentWorkflowActor.prolongPayment(payment)
      probe.expectMsgType[PaymentDataActor.UpdateExpireDate]
      probe reply 0
      resultFtr.awaitResult === false
    }
    "return false if expireDate update has succeeded" >> {
      val resultFtr = paymentWorkflowActor.prolongPayment(payment)
      probe.expectMsgType[PaymentDataActor.UpdateExpireDate]
      probe reply 1
      resultFtr.awaitResult === true
    }
  }

  "PaymentWorkflowActor change status" should {
    "reject payment when RejectPayment comes" >> {
      val f = new PaymentWorkflowActorFixture.WithConfirmRejectPaymentMock
      f.paymentWorkflowActor probeTells RejectPayment(f.OrderId) expectMsg JustChanged
      success
    }
    "confirm payment when ConfirmPayment comes" >> {
      val f = new PaymentWorkflowActorFixture.WithConfirmRejectPaymentMock
      f.paymentWorkflowActor probeTells ConfirmPayment(f.OrderId, f.TransactionId) expectMsg JustChanged
      success
    }
    "create present when ConfirmPayment returns JustChanged" >> {
      val f = new PaymentWorkflowActorFixture.WithConfirmRejectPaymentMock {
        val presentMade = Promise[Boolean]()
        override def makePresentStub(orderId: Payment.OrderId) {
          presentMade success true
        }
      }
      f.paymentWorkflowActor ! ConfirmPayment(f.OrderId, f.TransactionId)
      f.presentMade.awaitResult === true
    }
  }
  
  "PaymentWorkflowActor.recipientContacts" should {
    "get recipient contacts from user if userId provided" >> {
      val f = new PaymentWorkflowRecipientContactsFixture
      val future = f.paymentWorkflowActor.recipientContacts(f.recipientUserIdOnly)
      f.userDataProbe.expectNoMsg()
      future.awaitResult === f.contactsByUserId
    }
    "get recipient contacts from request if no userId provided" >> {
      val f = new PaymentWorkflowRecipientContactsFixture
      import f._
      "userId NOT found by email, use just provided email" >> {
        val future = paymentWorkflowActor.recipientContacts(recipientEmailOnly)
        userDataProbe expectMsg UserDataActor.FindUserIdByEmail(contacts.email)
        userDataProbe reply None
        future.awaitResult === paymentWorkflowActor.RecipientContacts(recipientEmailOnly)
      }
      "userId have found by email, use contacts of found user" >> {
        val future = paymentWorkflowActor.recipientContacts(recipientEmailOnly)
        userDataProbe expectMsg UserDataActor.FindUserIdByEmail(contacts.email)
        userDataProbe reply Some(contactsByUserId.id.get)
        future.awaitResult === contactsByUserId
      }
      "userId NOT found by phone, use just provided phone number" >> {
        val future = paymentWorkflowActor.recipientContacts(recipientPhoneOnly)
        userDataProbe expectMsg UserDataActor.FindUserIdByPhone(recipientPhoneOnly.country.get.phone, recipientPhoneOnly.phone.get)
        userDataProbe reply None
        future.awaitResult === paymentWorkflowActor.RecipientContacts(recipientPhoneOnly)
      }
      "userId have found by phone, use contacts of found user and the main user's phone instead of provided one" >> {
        val future = paymentWorkflowActor.recipientContacts(recipientPhoneOnly)
        userDataProbe expectMsg UserDataActor.FindUserIdByPhone(recipientPhoneOnly.country.get.phone, recipientPhoneOnly.phone.get)
        userDataProbe reply Some(contactsByUserId.id.get)
        future.awaitResult === contactsByUserId
      }
    }
  }

  "PaymentWorkflowActor.checkAvailability" should {
    "return product if availability is not specified" >> {
      val f = new PaymentWorkflowActorCheckAvailabilityFixture
      val product = new ProductFixture().product.copy(availabilityCount = None)
      f.paymentWorkflowActor.checkAvailability(product).awaitResult === product
    }
    "return product if product is available" >> {
      val f = new PaymentWorkflowActorCheckAvailabilityFixture {
        override def openPayments = 1
      }
      val product = new ProductFixture().product.copy(availabilityCount = Some(2))
      f.paymentWorkflowActor.checkAvailability(product).awaitResult === product
    }
    "return failure if product is not available" >> {
      val f = new PaymentWorkflowActorCheckAvailabilityFixture {
        override def openPayments = 2
      }
      val product = new ProductFixture().product.copy(availabilityCount = Some(2))
      f.paymentWorkflowActor.checkAvailability(product).awaitResult must throwA[ApplicationException]
    }
  }

  "PaymentWorkflowActor.confirm" should {
    "decrement product availability before confirming" >> {
      val f = new PaymentWorkflowActorFixture.WithConfirmRejectPaymentMock {
        val decremented = Promise[Boolean]()
        override def decrementProductAvailabilityStub(orderId: Payment.OrderId) = {
          decremented success true
          super.decrementProductAvailabilityStub(orderId)
        }

        val checked = Promise[Boolean]()
        override def confirmPaymentStub(orderId: Payment.OrderId, transactionId: String) = {
          if (decremented.awaitResult) checked success true
          super.confirmPaymentStub(orderId, transactionId)
        }
      }
      f.paymentWorkflowActor ! ConfirmPayment(f.OrderId, f.TransactionId)
      f.decremented.awaitResult === true
      f.checked.awaitResult === true
      todo
    }
    "increment after confirmation if it was AlreadyChanged and decremented earlier" >> {
      val f = new PaymentWorkflowActorFixture.WithConfirmRejectPaymentMock {
        val incremented = Promise[Boolean]()
        val productId = ProductId(new ObjectId())
        override def decrementProductAvailabilityStub(orderId: Payment.OrderId) = Future successful Some(productId)
        override def confirmPaymentStub(orderId: Payment.OrderId, transactionId: String) = Future successful AlreadyChanged
        override def incrementProductAvailabilityStub(prodId: ProductId) = {
          assert(prodId == productId)
          incremented success true
        }
      }
      f.paymentWorkflowActor ! ConfirmPayment(f.OrderId, f.TransactionId)
      f.incremented.awaitResult === true
    }
  }

  step {
    system.shutdown()
  }
}

class PaymentWorkflowRecipientContactsFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  import com.tooe.core.util.SomeWrapper._
  
  val regionId = RegionId(new ObjectId)
  val countryId = CountryId(HashHelper.uuid.take(2))
  
  val countryCode = "+7"
  val phoneNumber = "23423t5345634"
  
  val recipientUserIdOnly = Recipient(
    id = UserId(new ObjectId),
    email = None,
    phone = None,
    country = None
  )

  val recipientEmailOnly = Recipient(
    id = None,
    email = "some-email",
    phone = None,
    country = None
  )

  val recipientPhoneOnly = Recipient(
    id = None,
    email = None,
    phone = "some-phone-number",
    country = CountryParams(countryId, "some-phone-code")
  )

  val contacts = UserContact(
    address = UserAddress(
      countryId = CountryId("country-id"),
      regionId = regionId,
      regionName = "",
      country = ""
    ),
    phones = UserPhones(all = Nil, main = Phone(countryCode = countryCode, number = phoneNumber, purpose = None)),
    email = "some-email"
  )

  val userDataProbe = TestProbe()

  def paymentWorkflowActorFactory = new PaymentWorkflowActor(
    productDataActor = null,
    locationDataActor = null,
    paymentDataActor = null
  ) {
    override lazy val userDataActor = userDataProbe.ref

    override def getUserContacts(id: UserId) = Future successful contacts
    override def getUserCountryId(id: RegionId) = Future successful countryId
  }
  
  lazy val paymentWorkflowActor = TestActorRef[PaymentWorkflowActor](Props(paymentWorkflowActorFactory)).underlyingActor
  
  val contactsByUserId = new paymentWorkflowActor.RecipientContacts(
    id = recipientUserIdOnly.id,
    email = contacts.email,
    phone = phoneNumber,
    countryId = countryId,
    phoneCode = countryCode
  )
}

object PaymentWorkflowActorFixture {

  class WithConfirmRejectPaymentMock(implicit actorSystem: ActorSystem) extends PaymentWorkflowActorFixture() {
    val TransactionId = HashHelper.uuid

    def productDataActorRef: ActorRef = null
    def locationActorRef: ActorRef = null
    def presentActorRef: ActorRef = null
    def paymentDataActorRef: ActorRef = null

    override def confirmPaymentStub(orderId: Payment.OrderId, transactionId: String) = {
      assert(orderId == OrderId)
      assert(transactionId == TransactionId)
      super.confirmPaymentStub(orderId, transactionId)
    }

    override def rejectPaymentStub(orderId: Payment.OrderId): Future[StateChangeResult] = {
      assert(orderId == OrderId)
      super.rejectPaymentStub(orderId)
    }
  }

  class WithGetPaymentMock(prolongPaymentResult: Boolean = true)(implicit actorSystem: ActorSystem) extends PaymentWorkflowActorFixture() {
    val prolongProbe = TestProbe()

    override def paymentFactory = Payment()
    def productDataActorRef = null
    def locationActorRef = null
    def presentActorRef = null
    def paymentDataActorRef: ActorRef = null

    override def prolongPaymentStub(payment: Payment) = {
      prolongProbe.ref ! payment
      Future successful prolongPaymentResult
    }

    override def getPaymentStub(orderId: Payment.OrderId) = {
      assert(orderId == OrderId)
      val payment = paymentFactory
      payment.orderJpaId = OrderId.bigInteger
      Future successful payment
    }
  }
}

abstract class PaymentWorkflowActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  import com.tooe.core.util.DateHelper._

  val expiresInMinutes = 1000

  val OrderId = BigInt(Random.nextLong())
  val paymentDate = new Timestamp(2394723905L)
  val expiresAt = new Timestamp(paymentDate.addMinutes(expiresInMinutes).getTime)

  def paymentFactory: Payment = ???
  
  def checkProductAvailabilityStub(product: Product) = Future successful product
  def decrementProductAvailabilityStub(orderId: Payment.OrderId): Future[Option[ProductId]] = Future successful None
  def confirmPaymentStub(orderId: Payment.OrderId, transactionId: String): Future[StateChangeResult] = Future successful JustChanged
  def rejectPaymentStub(orderId: Payment.OrderId): Future[StateChangeResult] = Future successful JustChanged
  def incrementProductAvailabilityStub(productId: ProductId): Unit = ()
  def makePresentStub(orderId: Payment.OrderId): Unit = ()
  def getPaymentStub(orderId: Payment.OrderId): Future[Payment] = Future successful paymentFactory
  def checkPriceStub(amount: Amount, price: Price): Future[_] = Future successful ()
  def prolongPaymentStub(payment: Payment): Future[Boolean] = Future successful true

  def productDataActorRef: ActorRef
  def locationActorRef: ActorRef
  def paymentDataActorRef: ActorRef
  def presentActorRef: ActorRef

  def paymentWorkflowActorFactory = new PaymentWorkflowActorUnderTest
    
  class PaymentWorkflowActorUnderTest extends PaymentWorkflowActor(
    productDataActor = productDataActorRef,
    locationDataActor = locationActorRef,
    paymentDataActor = paymentDataActorRef
  ) {
    override lazy val presentWriteActor = presentActorRef
    override def getCurrentDate(): Date = paymentDate
    override def getPaymentExpiresInMins: Int = expiresInMinutes
    override def checkAvailability(product: Product) = checkProductAvailabilityStub(product)
    override def confirmPayment(orderId: Payment.OrderId, transactionId: String, fullPrice: Option[BigDecimal]) =
      confirmPaymentStub(orderId, transactionId)
    override def rejectPayment(orderId: Payment.OrderId) = rejectPaymentStub(orderId)
    override def makePresent(orderId: Payment.OrderId) = makePresentStub(orderId)
    override def decrementProductAvailability(orderId: Payment.OrderId) = decrementProductAvailabilityStub(orderId)
    override def incrementProductAvailability(productId: ProductId) = incrementProductAvailabilityStub(productId)
    override def getPayment(orderId: Payment.OrderId): Future[Payment] = getPaymentStub(orderId)
    override def checkPrice(amount: Amount, price: Price)(implicit lang: Lang): Future[_] = checkPriceStub(amount, price)
    override def prolongPayment(payment: Payment) = prolongPaymentStub(payment)
    override def findUserIdByEmail(email: String) = Future successful None
    override def findUserIdByPhone(code: String, number: String) = Future successful None
  }

  lazy val paymentWorkflowActor = TestActorRef[PaymentWorkflowActor](Props(paymentWorkflowActorFactory))
}

class PaymentWorkflowActorCheckAvailabilityFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val MsgException = ApplicationException()

  def openPayments = 3

  def paymentWorkflowActorFactory = new PaymentWorkflowActor(
    productDataActor = null,
    locationDataActor = null,
    paymentDataActor = null
  ) {
    override def getFailure(id: String)(implicit lang: Lang) = Future failed MsgException

    override def countOpenPayments(id: ProductId) = Future successful openPayments
  }

  lazy val paymentWorkflowActorRef = TestActorRef[PaymentWorkflowActor](Props(paymentWorkflowActorFactory))

  lazy val paymentWorkflowActor = paymentWorkflowActorRef.underlyingActor
}

class PaymentWorkflowActorProlongPaymentFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  import DateHelper._

  val dateGen = new TestDateGenerator(step = 500000, multiplicity = 1000)

  val payment = new PaymentFixture(
    isDeleted = false,
    date = dateGen.next().toTimestamp,
    expireDate = dateGen.next().toTimestamp
  ).payment

  val probe = TestProbe()

  def paymentWorkflowActorFactory: PaymentWorkflowActor =  new PaymentWorkflowActor(
    productDataActor = null,
    locationDataActor = null,
    paymentDataActor = probe.ref
  )

  lazy val paymentWorkflowActorRef = TestActorRef[PaymentWorkflowActor](Props(paymentWorkflowActorFactory))

  lazy val paymentWorkflowActor: PaymentWorkflowActor = paymentWorkflowActorRef.underlyingActor
}

class ProductFixture(price: BigDecimal = BigDecimal("231.35"), discountPercent: Int = 0) {
  val pictureUrl = "some-product-picture-url"

  val product = Product(
    id = ProductId(new ObjectId),
    companyId = CompanyId(),
    productTypeId = ProductTypeId.Product,
    price = Price(price, CurrencyId("RUR")),
    discount = if (discountPercent == 0) None else Some(Discount(percent = discountPercent)),
    location = LocationWithName(LocationId(new ObjectId()), Map("orig" -> "name")),
    regionId = RegionId(new ObjectId()),
    name = Map("orig" -> "name"),
    description = Map("orig" -> "name"),
    validityInDays = 10,
    productMedia = ProductMedia(MediaObject(MediaObjectId(pictureUrl))) :: Nil,
    availabilityCount = Some(100),
    article = Some(HashHelper.uuid.take(50))
  )
}