package com.tooe.core.usecase.period

import com.tooe.core.application.Actors
import com.tooe.core.usecase.AppActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.PeriodDataService
import scala.concurrent.Future
import akka.pattern.pipe

object PeriodDataActor {
  final val Id = Actors.PeriodData

  case object FindAll

}

class PeriodDataActor extends AppActor {

  import PeriodDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[PeriodDataService]

  def receive = {
    case FindAll => Future(service.findAll).pipeTo(sender)
  }

}
