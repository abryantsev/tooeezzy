package com.tooe.core.usecase.admin_role

import com.tooe.core.application.Actors
import com.tooe.core.usecase.AppActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.AdminRoleDataService
import scala.concurrent.Future
import akka.pattern.pipe

object AdminRoleDataActor {
  final val Id = Actors.AdminRoleData

  case object FindAll

}

class AdminRoleDataActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminRoleDataActor._

  lazy val service = BeanLookup[AdminRoleDataService]

  def receive = {
    case FindAll => Future(service.findAll).pipeTo(sender)
  }

}
