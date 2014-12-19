package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.usecase.users_group.UsersGroupDataActor
import akka.pattern.{ask, pipe}
import com.tooe.api.service.SuccessfulResponse
import com.tooe.core.db.mongo.domain.UsersGroup
import com.tooe.core.domain.UsersGroupId
import com.tooe.core.util.Lang

object UsersGroupActor {
  final val Id = Actors.UsersGroup

  case class GetAllUsersGroups(lang: Lang)

}

class UsersGroupActor extends AppActor {

  import UsersGroupActor._

  lazy val userGroupDataActor = lookup(UsersGroupDataActor.Id)
  implicit lazy val ec = scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case GetAllUsersGroups(lang) =>
      implicit val l = lang
      val result = for {
        groups <- getAllUsersGroups
        sorted = groups.sortWith(_.name.localized.getOrElse("") < _.name.localized.getOrElse(""))
      } yield {
        GetAllUserGroupsResponse(sorted.map(i => GetAllUserGroupsResponseItem(i.id, i.name.localized.getOrElse(""))))
      }
      result.pipeTo(sender)
  }

  def getAllUsersGroups =
    userGroupDataActor.ask(UsersGroupDataActor.FindAll).mapTo[Seq[UsersGroup]]
}

case class GetAllUserGroupsResponse(usergroups: Seq[GetAllUserGroupsResponseItem]) extends SuccessfulResponse

case class GetAllUserGroupsResponseItem(id: UsersGroupId, name: String)
