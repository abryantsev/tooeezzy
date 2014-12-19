package com.tooe.core.usecase.job

import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.api.service.ExecutionContextProvider
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.PresentDataService

object ExpiredPresentsMarkerActor {
  val Id = Actors.ExpiredPresentsMarker

  case object CheckPresents

}

class ExpiredPresentsMarkerActor extends AppActor with ExecutionContextProvider {

  import ExpiredPresentsMarkerActor._

  lazy val service = BeanLookup[PresentDataService]

  override def receive = {
    case CheckPresents =>
      log.info("ExpiredPresentsMarkerActor has started")
      val updated = service.updatePresentStatusForExpiredPresents()
      log.info(s"ExpiredPresentsMarkerActor has finished. $updated presents have been updated.")
  }
}
