package com.tooe.core.usecase.currency

import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CurrencyDataService
import scala.concurrent.Future
import akka.pattern.pipe

object CurrencyDataActor {

  final val Id = Actors.CurrencyData

  case object FindAll

}

class CurrencyDataActor extends AppActor {

  import CurrencyDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[CurrencyDataService]

  def receive = {
    case FindAll => Future(service.findAll).pipeTo(sender)
  }

}
