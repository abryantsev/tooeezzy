package com.tooe.core.usecase.moderation_status

import com.tooe.core.application.Actors
import com.tooe.core.usecase.AppActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.ModerationStatusDataService
import scala.concurrent.Future
import akka.pattern.pipe

object ModerationStatusDataActor {
  final val Id = Actors.ModerationStatusData

  case object FindAll

}

class ModerationStatusDataActor extends AppActor {

  import ModerationStatusDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[ModerationStatusDataService]

  def receive = {
    case FindAll => Future(service.findAll).pipeTo(sender)
  }

}
