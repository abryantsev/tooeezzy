package com.tooe.core.usecase.users_group

import com.tooe.core.usecase.AppActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.UsersGroupDataService
import scala.concurrent.Future
import akka.pattern.pipe
import com.tooe.core.application.Actors

object UsersGroupDataActor {
  final val Id = Actors.UsersGroupData

  case object FindAll

}

class UsersGroupDataActor extends AppActor {

  import UsersGroupDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[UsersGroupDataService]

  def receive = {
    case FindAll => Future(service.findAll).pipeTo(sender)
  }

}