package com.tooe.core.usecase.payment

import com.tooe.core.util.ActorHelper
import com.tooe.core.application.{Actors, AppActors}
import akka.actor.Actor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.db.mysql.services.PaymentDataService
import scala.concurrent.Future
import com.tooe.core.exceptions.NotFoundException
import akka.pattern.pipe
import com.tooe.core.domain.ProductId
import java.util.Date
import com.tooe.core.usecase.ExportOrdersCompleteRequest

object PaymentDataActor {
  final val Id = Actors.PaymentData

  case class SavePayment(payment: Payment)
  case class GetAllPayments()
  case class GetPayment(orderId: Payment.OrderId)
  case class GetPayments(orderIds: Seq[Payment.OrderId])
  case class GetPaymentByUuid(orderUuid: String)
  case class RejectPayment(orderId: Payment.OrderId)
  case class ConfirmPayment(orderId: Payment.OrderId, transactionId: String, fullPrice: Option[BigDecimal])
  case class CountOpenPayments(productId: ProductId)
  case class ActivatePayment(orderId: Payment.OrderId)
  case class GetPaymentsForExport(date: Date)
  case class GetPaymentsForExportCount(date: Date)
  case class MarkPaymentAsPotentialExported(orderIds: List[Payment.OrderId], date: Date)
  case class MarkOrderExportComplete(request: ExportOrdersCompleteRequest, date: Date)
  case class UpdateExpireDate(orderId: Payment.OrderId, expireDate: Date)
}

class PaymentDataActor extends Actor with ActorHelper with AppActors {

  lazy val service = BeanLookup[PaymentDataService]

  import PaymentDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case SavePayment(payment) => sender ! service.savePayment(payment)
    case GetAllPayments() => sender ! service.allPayments
    case GetPayment(orderId) => Future {
      service.findPayment(orderId) getOrElse (throw NotFoundException(s"Payment(orderId=${orderId.toString}) not found"))
    } pipeTo sender
    case GetPaymentByUuid(uuid) => Future {
      service.findPayment(uuid) getOrElse (throw NotFoundException(s"Payment(uuid=$uuid) not found"))
    } pipeTo sender
    case RejectPayment(orderId) => Future {
      service.reject(orderId)
    } pipeTo sender
    case ConfirmPayment(orderId, transactionId, fullPrice) => Future {
      service.confirm(orderId, transactionId, fullPrice)
    } pipeTo sender
    case CountOpenPayments(productId) => Future {
      service.countOpenPayments(productId)
    } pipeTo sender
    case ActivatePayment(orderId) => Future {
      service.activate(orderId)
    } pipeTo sender
    case GetPayments(ids) =>  Future(service.findPayments(ids)).pipeTo(sender)
    case GetPaymentsForExport(date) => Future { service.getForExport(date) } pipeTo sender
    case GetPaymentsForExportCount(date) => Future { service.getForExportCount(date) } pipeTo sender
    case MarkPaymentAsPotentialExported(orderIds, date) => Future { service.markAsTryExport(date, orderIds) }
    case MarkOrderExportComplete(request, date) => Future { service.markOrderExportComplete(request, date) }
    case UpdateExpireDate(orderId, expireDate) => Future(service.updateExpireDate(orderId, expireDate)) pipeTo sender
  }
}