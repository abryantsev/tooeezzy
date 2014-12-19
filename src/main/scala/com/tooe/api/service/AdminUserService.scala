package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.domain._
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.api.validation.{ValidationContext, Validatable}
import com.tooe.core.usecase.admin_user.{AdminUserReadActor, AdminUserWriteActor}
import spray.http.StatusCodes
import com.tooe.core.usecase.admin_user.AdminUserWriteActor._
import com.tooe.core.usecase.admin_user.AdminUserWriteActor.ChangeAdminUser
import com.tooe.core.domain.AdminUserId
import com.tooe.core.usecase.admin_user.AdminUserWriteActor.CreateAdminUser

class AdminUserService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import AdminUserService._

  lazy val adminUserWriteActor = lookup(AdminUserWriteActor.Id)
  lazy val adminUserReadActor = lookup(AdminUserReadActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
    authenticateAdminBySession { implicit s: AdminUserSession =>
        pathEndOrSingleSlash {
          authorizeByRole(AdminRoleId.Admin) {
          post {
            entity(as[AdminUserRegisterRequest]) { request: AdminUserRegisterRequest =>
              complete(StatusCodes.Created,
                (adminUserWriteActor ? CreateAdminUser(request.copy(email = request.email.toLowerCase.trim))).mapTo[SuccessfulResponse])
            }
          }
          } ~
          get {
            complete(adminUserReadActor.ask(AdminUserReadActor.GetAdminUserInfo(s.adminUserId)).mapTo[SuccessfulResponse])
          }
        } ~
        authorizeByRole(AdminRoleId.Admin) {
        path(Segment).as(AdminUserId) { adminUserId: AdminUserId =>
          get {
            complete(
              (adminUserReadActor ? AdminUserReadActor.GetAdminUserInfo(adminUserId)).mapTo[SuccessfulResponse])
          } ~
          post {
            entity(as[AdminUserChangeRequest]) { request: AdminUserChangeRequest =>
              complete((adminUserWriteActor ? ChangeAdminUser(adminUserId, request)).mapTo[SuccessfulResponse])

            }
          } ~
          delete {
            complete(
              (adminUserWriteActor ? DeleteAdminUser(adminUserId)).mapTo[SuccessfulResponse])
          }
        } ~
        path(search) {
          (get & offsetLimit) { offsetLimit: OffsetLimit =>
            parameters('name ?, 'sort.as[AdminUserSort] ?, 'showall.as[Boolean] ?) {
              (name: Option[String], sort: Option[AdminUserSort], showAll: Option[Boolean]) =>
                val searchRequest = SearchAdminUserRequest(name, sort, showAll)
                searchRequest.check
                complete((adminUserReadActor ? AdminUserReadActor.SearchAdminUser(searchRequest, offsetLimit, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    }
  }

}

object AdminUserService {
  val Root = "admusers"
  val search = "admsearch"
}

case class AdminUserRegisterRequest(name: String,
                                    @JsonProperty("lastname") lastName: String,
                                    email: String,
                                    @JsonProperty("pwd") password: String,
                                    role: AdminRoleId,
                                    description: Option[String]) extends UnmarshallerEntity with Validatable {
  def validate(ctx: ValidationContext) {
    if(role == AdminRoleId.Activator && description.isEmpty)
      ctx.fail("You must set description for role Activator")
  }
}

case class AdminUserChangeRequest(name: Option[String],
                                  @JsonProperty("lastname") lastName: Option[String],
                                  email: Option[String],
                                  @JsonProperty("pwd") password: Option[String],
                                  role: Option[AdminRoleId],
                                  description: Unsetable[String]) extends UnmarshallerEntity

case class SearchAdminUserRequest(name: Option[String], sort: Option[AdminUserSort], showAll: Option[Boolean] = None) extends Validatable {

  def validate(ctx: ValidationContext) {
    name.map { name =>
    if(name.length < 3)
      ctx.fail("Name length must be greater that 3")
    }
  }

}