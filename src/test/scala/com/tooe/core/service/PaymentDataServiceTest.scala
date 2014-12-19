package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mysql.services.{StateChangeResult, PaymentDataService}
import com.tooe.core.util.{TestDateGenerator, HashHelper}
import com.tooe.core.db.mysql.domain.{Recipient, PaymentSystem, ProductSnapshot, Payment}
import java.sql.Timestamp
import java.util.Date
import org.bson.types.ObjectId
import com.tooe.core.domain.{UserId, ExportStatus, ProductTypeId, ProductId}
import com.tooe.core.usecase.ExportOrdersCompleteRequest

class PaymentDataServiceTest extends SpringDataMySqlTestHelper {
  @Autowired var service: PaymentDataService = _

  @Test def readWrite() {
    val f = new PaymentFixture
    import f._
    val orderId = service.savePayment(payment).orderId
    val result = service.findPayment(orderId).orNull
    result !== null
    result.productSnapshot !== null
    result.paymentSystem !== null
    result.recipient !== null
    result.orderId !== 0L
  }

  @Test def findByUuid() {
    val f = new PaymentFixture
    import f._
    service.findPayment(payment.uuid) === None
    service.savePayment(payment)
    service.findPayment(payment.uuid).get.orderId === payment.orderId
  }

  @Test def autoincReceiveCode() {
    val f = new PaymentFixture
    import f._
    val saved = service.savePayment(payment)
    saved.orderId !== 0L
  }

  @Test def activate() {
    val f = new PaymentFixture
    import f._
    val orderId = service.savePayment(payment).orderId
    service.activate(orderId)
    val updated = service.findPayment(orderId)
    updated !== None
    updated.map(_.isReceived) === Some(true)
  }

  @Test def update() {
    val f = new PaymentFixture
    import f._
    val result = service.savePayment(payment)
    result.isRejected === false

    val orderId = result.orderId

    val saved = service.findPayment(orderId).get
    saved.rejected = true
    service.savePayment(saved)

    service.findPayment(orderId).get.isRejected === true
  }

  import StateChangeResult._

  @Test def reject() {
    val f = new PaymentFixture
    import f._
    val result = service.savePayment(payment)
    service.reject(result.orderId) === JustChanged
    service.reject(result.orderId) === AlreadyChanged
    service.findPayment(result.orderId).map(_.isRejected) === Some(true)
    service.confirm(result.orderId, HashHelper.uuid) === AlreadyChanged
    service.reject(BigInt(-1)) === PaymentNotFound
  }

  @Test def confirm() {
    val f = new PaymentFixture
    import f._
    val result = service.savePayment(payment)
    service.findPayment(result.orderId).get.transactionId === null
    val transactionId = HashHelper.uuid
    val fullPrice = Some(BigDecimal(333.33))
    service.confirm(result.orderId, transactionId, fullPrice) === JustChanged
    val found = service.findPayment(result.orderId).get
    found.transactionId === transactionId
    found.amount === fullPrice.get.bigDecimal
    service.confirm(result.orderId, HashHelper.uuid) === AlreadyChanged
    service.reject(result.orderId) === AlreadyChanged
    service.confirm(BigInt(-1), HashHelper.uuid) === PaymentNotFound
  }

  @Test
  def finAllByIds() {
   val payments = (1 to 3).map(_ => service.savePayment(new PaymentFixture().payment)).toSeq

   val result = service.findPayments(payments.map(_.orderId))

   result.size === payments.size
   result.zip(payments).foreach{
     case (found, expected) => found.uuid === expected.uuid
   }
  }
  
  @Test def countOpenTransactions() {
    val f0anotherProduct = new PaymentFixture()
    val productId = ProductId(new ObjectId)
    val f1, f2 = new PaymentFixture(productId)
    val f3deleted = new PaymentFixture(productId, isDeleted = true)

    service.savePayment(f3deleted.payment)

    service.savePayment(f0anotherProduct.payment)
    service.countOpenPayments(productId) === 0
   
    val p1 = service.savePayment(f1.payment)
    val p2 = service.savePayment(f2.payment)
    service.countOpenPayments(productId) === 2

    service.reject(p1.orderId)
    service.countOpenPayments(productId) === 1
    
    service.confirm(p2.orderId, HashHelper.uuid)
    service.countOpenPayments(productId) === 0
  }

  @Test
  def getOverduePayments {
    import com.tooe.core.util.DateHelper._
    val runDate = new Date(80, 1, 1, 0, 0, 0)
    val expireDate = new Timestamp(runDate.addSeconds(-1).getTime)
    val validDate = new Timestamp(runDate.addSeconds(+1).getTime)
    val p1, p2 = new PaymentFixture(expireDate = expireDate).payment
    val alreadyFinishedPayment = new PaymentFixture(expireDate = expireDate, transactionId = Some(HashHelper.uuid)).payment
    val notExpiredPayment = new PaymentFixture(expireDate = validDate).payment

    val ps = Seq(p1, p2, alreadyFinishedPayment, notExpiredPayment)

    for (p <- ps) {
      p.isDeleted === false
      service.savePayment(p)
    }

    service.markDeleteOverduePayments(runDate) === 2

    service.findPayment(p1.orderId).get.isDeleted === true
    service.findPayment(p2.orderId).get.isDeleted === true
    service.findPayment(alreadyFinishedPayment.orderId).get.isDeleted === false
    service.findPayment(notExpiredPayment.orderId).get.isDeleted === false
  }

  @Test
  def getForExport {

    def validate(p: Payment, date: Date) {
      p.exportUpdateTime must beNull
      p.exportCount must lessThan(5)
      if(!(p.exportTime == null || p.exportTime.before(date)))
        assert(false, "Invalid payment entity for export: incorrect exportTime value")
      p.isDeleted must beFalse
      p.isRejected must beFalse
    }

    val startTestTime = new Date

    val paymentForExport = service.getForExport(startTestTime)
    val paymentsCount = service.getForExportCount(startTestTime)

    paymentForExport.size must beLessThanOrEqualTo(100)
    paymentForExport.size must lessThanOrEqualTo(paymentsCount)

    paymentForExport.foreach(validate(_, startTestTime))

    paymentForExport === paymentForExport.sortBy(_.date.getTime)

  }

  @Test
  def markAsTryExport {
    val startTestTime = new Date

    val payment = new PaymentFixture().payment

    service.savePayment(payment)

    service.markAsTryExport(startTestTime, List(payment.orderJpaId)) === 1

    val exportedPayment = service.findPayment(payment.orderJpaId)
    exportedPayment.map(_.exportCount) === Some(payment.exportCount + 1)
    //hide milliseconds
    exportedPayment.map(_.exportTime).map(_.getTime / 1000) === Some(startTestTime.getTime / 1000)
  }

  @Test
  def markOrderExportComplete {
    val startTestTime = new Date

    val payment = new PaymentFixture().payment

    service.savePayment(payment)

    service.markOrderExportComplete(ExportOrdersCompleteRequest(List(payment.orderJpaId), ExportStatus.Exported), startTestTime)

    val exportedPayment = service.findPayment(payment.orderJpaId)
    exportedPayment.map(_.exportUpdateTime.getTime / 1000) === Some(startTestTime.getTime / 1000)
  }

  @Test
  def updateExpireDate {
    val f = new UpdatePaymentExpireDateFixture(deleted = false)
    import f._
    service.savePayment(payment)
    service.findPayment(payment.orderId).get.getExpireDate === d1

    service.updateExpireDate(payment.orderId, d2) === 1
    service.findPayment(payment.orderId).get.getExpireDate === d2
  }

  @Test
  def dontUpdateExpireDate {
    val f = new UpdatePaymentExpireDateFixture(deleted = true)
    import f._
    service.savePayment(payment)

    service.updateExpireDate(payment.orderId, d2) === 0
    service.findPayment(payment.orderId).get.getExpireDate === d1
  }
}

object PaymentFixture {
  def timestamp() = new Timestamp(new Date().getTime)
}

class PaymentFixture
(
  val productId: ProductId = ProductId(new ObjectId),
  date: Timestamp = PaymentFixture.timestamp(),
  expireDate: Timestamp = PaymentFixture.timestamp(),
  transactionId: Option[String] = None,
  isDeleted: Boolean = false,
  recipientId: Option[UserId] = Some(UserId())
  )
{
  private def randomUUID = HashHelper.uuid

  def paymentUuid = randomUUID

  def objectId() = new ObjectId().toString
  
  val payment = Payment(
    userId = objectId(),
    amount = new java.math.BigDecimal("9.99"),
    currencyId = randomUUID.take(5),
    expireDate = expireDate,
    uuid = paymentUuid
  )
  payment.transactionId = transactionId.orNull
  payment.date = date
  payment.exportTime = new Timestamp(System.currentTimeMillis())
  payment.deleted = isDeleted

  val productSnapshot = ProductSnapshot(
    payment,
    productId = productId.id.toString,
    product_type = ProductTypeId.Product.id,
    name = randomUUID,
    descr = "Payment.productSnapshot.descr",
    price = new java.math.BigDecimal(9.11),
    currency = randomUUID.take(5),
    validity = 10,
    companyId = objectId(),
    loc_id = objectId(),
    loc_name = randomUUID,
    loc_city = randomUUID,
    loc_country = randomUUID.take(20),
    loc_street = randomUUID,
    picture_url = randomUUID.take(200),
    present_msg = randomUUID.take(3000),
    article = randomUUID.take(50)
  )

  val paymentSystem = PaymentSystem (
    payment,
    system = "Payment.paymentSystem.system",
    subSystem = randomUUID
  )

  val recipient = Recipient(
    payment,
    recipientId = recipientId.map(_.id.toString).orNull,
    email = randomUUID.take(50),
    phone = randomUUID.take(20),
    countryId = randomUUID.take(2),
    phoneCode = randomUUID.take(10),
    showActor = true
  )
}

class UpdatePaymentExpireDateFixture(deleted: Boolean) {
  val dateGenerator = new TestDateGenerator()
  val d1, d2 = dateGenerator.next(step = 50000, multiplicity = 1000)

  val payment = new PaymentFixture(expireDate = new Timestamp(d1.getTime), isDeleted = deleted).payment
}