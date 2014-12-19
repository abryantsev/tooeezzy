package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.util.Lang
import com.tooe.core.usecase.admin_role.AdminRoleDataActor
import akka.pattern.{pipe, ask}
import com.tooe.core.db.mongo.domain.AdminRole
import com.tooe.api.service.SuccessfulResponse


object AdminRoleActor {
  final val Id = Actors.AdminRole

  case class GetAllAdminRoles(lang: Lang)

}

class AdminRoleActor extends AppActor {

  import AdminRoleActor._

  lazy val adminRoleDataActor = lookup(AdminRoleDataActor.Id)

  implicit val ec = scala.concurrent.ExecutionContext.global

  def receive = {
    case GetAllAdminRoles(lang) =>
      implicit val l = lang
      val result = for {
        roles <- getAllAdminRoles
        sorted = roles.sortWith(_.name.localized.getOrElse("") < _.name.localized.getOrElse(""))
      } yield {
        GetAllAdminRolesResponse(sorted.map(role => GetAllAdminRolesResponseItem(role.id.id, role.name.localized.getOrElse(""))))
      }
      result.pipeTo(sender)

  }

  def getAllAdminRoles =
    adminRoleDataActor.ask(AdminRoleDataActor.FindAll).mapTo[Seq[AdminRole]]

}

case class GetAllAdminRolesResponse(admuserroles: Seq[GetAllAdminRolesResponseItem]) extends SuccessfulResponse

case class GetAllAdminRolesResponseItem(id: String, name: String)
