package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.domain.ExportStatus
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.payment.PaymentDataActor
import com.tooe.api.service.SuccessfulResponse
import java.util.Date

object OrderWriteActor {
  final val Id = Actors.OrderWrite

  case class OrdersExportComplete(request: ExportOrdersCompleteRequest)
}

class OrderWriteActor extends AppActor {

  import OrderWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val paymentDataActor = lookup(PaymentDataActor.Id)

  def receive = {
    case OrdersExportComplete(request) =>
      paymentDataActor ! PaymentDataActor.MarkOrderExportComplete(request, new Date)
      sender ! SuccessfulResponse
  }

}

case class ExportOrdersCompleteRequest(ids: Seq[Payment.OrderId], status: ExportStatus) extends UnmarshallerEntity