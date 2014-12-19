package com.tooe.core.usecase.job

import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.db.mysql.services.PaymentDataService
import com.tooe.core.util.DateHelper

object OverduePaymentCancellationActor {
  final val Id = Actors.OverduePaymentCancellation

  case class StartJob()
}

class OverduePaymentCancellationActor extends AppActor {

  import OverduePaymentCancellationActor._

  lazy val service = BeanLookup[PaymentDataService]

  def receive = {
    case StartJob() =>
      log.info("OverduePaymentCancellationActor has started")
      val updatedQty = service.markDeleteOverduePayments(DateHelper.currentDate)
      log.info(s"OverduePaymentCancellationActor has finished. $updatedQty payments have been updated.")
  }
}