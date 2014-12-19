package com.tooe.core.usecase.lifecycle_status

import com.tooe.core.application.Actors
import com.tooe.core.usecase.AppActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.LifecycleStatusDataService
import scala.concurrent.Future
import akka.pattern.pipe

object LifecycleStatusDataActor {
  final val Id = Actors.LifecycleData

  case object FindAll

}

class LifecycleStatusDataActor extends AppActor {

  import LifecycleStatusDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[LifecycleStatusDataService]

  def receive = {
    case FindAll => Future(service.findAll).pipeTo(sender)
  }

}
