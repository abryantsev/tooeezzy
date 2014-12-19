package com.tooe.core.db.mysql.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.tooe.core.db.mysql.domain.Payment
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mysql.repository.PaymentRepository
import com.tooe.core.domain.ProductId
import java.util.Date
import com.tooe.core.db.mongo.query.SkipLimitSort
import com.tooe.core.usecase.ExportOrdersCompleteRequest
import java.sql.Timestamp

trait PaymentDataService {
  def savePayment(payment: Payment): Payment
  def allPayments: Seq[Payment]
  def findPayment(orderId: Payment.OrderId): Option[Payment]
  def findPayments(orderIds: Seq[Payment.OrderId]): Seq[Payment]
  def findPayment(uuid: String): Option[Payment]

  def reject(orderId: Payment.OrderId): StateChangeResult
  def confirm(orderId: Payment.OrderId, transactionId: String, fullPrice: Option[BigDecimal] = None): StateChangeResult
  
  def countOpenPayments(id: ProductId): Long
  def activate(orderId: Payment.OrderId): StateChangeResult

  def updateExpireDate(orderId: Payment.OrderId, expireDate: Date): Int

  def markDeleteOverduePayments(runDate: Date): Int

  def markAsTryExport(now: Date, ids: List[Payment.OrderId]): Int

  def getForExport(now: Date): List[Payment]
  def getForExportCount(now: Date): Int

  def markOrderExportComplete(request: ExportOrdersCompleteRequest, date: Date): Unit
}

sealed trait StateChangeResult
object StateChangeResult {
  case object JustChanged extends StateChangeResult
  case object AlreadyChanged extends StateChangeResult
  case object PaymentNotFound extends StateChangeResult
}

@Service
@Transactional(readOnly=true)
class PaymentDataServiceImpl extends PaymentDataService {
  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  @Autowired var paymentRepo: PaymentRepository = _

  @Transactional
  def savePayment(payment: Payment) = paymentRepo.save(payment)

  def allPayments: Seq[Payment] = paymentRepo.findAllWithJoin().asScala.toSeq

  @Transactional
  def findPayment(orderId: Payment.OrderId) = Option(paymentRepo.findOne(orderId.bigInteger))

  def findPayment(uuid: String) = paymentRepo.findByUuid(uuid).asScala.headOption

  def findPayments(orderIds: Seq[Payment.OrderId]) = paymentRepo.findAll(orderIds.map(_.bigInteger)).asScala.toSeq

  import StateChangeResult._

  @Transactional
  def reject(orderId: Payment.OrderId) = findPayment(orderId) map { payment =>
    if (payment.isFinished) AlreadyChanged
    else {
      payment.rejected = true
      savePayment(payment)
      JustChanged
    }
  } getOrElse PaymentNotFound

  @Transactional
  def confirm(orderId: Payment.OrderId, transactionId: String, fullPrice: Option[BigDecimal]) =
    findPayment(orderId) map { payment =>
      if (payment.isFinished) AlreadyChanged
      else {
        fullPrice foreach (fp => payment.amount = fp.bigDecimal)
        payment.transactionId = transactionId
        savePayment(payment)
        JustChanged
      }
    } getOrElse PaymentNotFound
  
  def countOpenPayments(id: ProductId) = paymentRepo.countOpenPayments(id.id.toString)

  @Transactional
  def activate(orderId: Payment.OrderId): StateChangeResult = findPayment(orderId) map { payment =>
    if (payment.isReceived) AlreadyChanged
    else {
      payment.isReceived = true
      savePayment(payment)
      JustChanged
    }
  } getOrElse PaymentNotFound

  @Transactional
  def updateExpireDate(orderId: Payment.OrderId, expireDate: Date) =
    paymentRepo.updateExpireDate(orderId.bigInteger, new Timestamp(expireDate.getTime))

  def markDeleteOverduePayments(runDate: Date) = {
    paymentRepo.markDeleteOverduePayments(runDate)
  }

  @Transactional(readOnly = true)
  def getForExport(now: Date) = paymentRepo.getForExport(now, SkipLimitSort(0, 100)).asScala.toList

  @Transactional(readOnly = true)
  def getForExportCount(now: Date) = paymentRepo.getForExportCount(now).toInt

  @Transactional
  def markAsTryExport(now: Date, ids: List[Payment.OrderId]) = paymentRepo.markAsTryExport(now, ids.map(_.bigInteger).asJava)

  @Transactional
  def markOrderExportComplete(request: ExportOrdersCompleteRequest, date: Date) = paymentRepo.markOrderExportComplete(request.ids.map(_.underlying()), date)
}