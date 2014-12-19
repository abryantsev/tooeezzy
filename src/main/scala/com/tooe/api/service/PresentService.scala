package com.tooe.api.service

import spray.routing.{RequestContext, PathMatcher}
import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase._
import com.tooe.api.validation.{ValidationContext, Validatable}
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.present.PresentAdminSearchSortType
import com.tooe.core.domain._
import com.tooe.core.usecase.ChangePresentStatusRequest
import com.tooe.core.db.mongo.util.UnmarshallerEntity

class PresentService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import PresentService._

  implicit val ec = scala.concurrent.ExecutionContext.global

  lazy val presentReadActor = lookup(PresentReadActor.Id)
  lazy val presentWriteActor = lookup(PresentWriteActor.Id)

  val presentAdminSearchOffset = getOffsetLimit()

  val route = (mainPrefix & pathPrefix(Root)) { implicit routeContext: RouteContext =>
    pathEndOrSingleSlash {
      (get & offsetLimit) { offsetLimit: OffsetLimit =>
        parameters('type.as[String] ?, 'status.as[String] ?).as(GetPresentParameters) { parameters: GetPresentParameters =>
          authenticateBySession { userSession: UserSession =>
            complete((presentReadActor ? PresentReadActor.GetPresents(userSession.userId, parameters, offsetLimit, routeContext)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    pathPrefix(ObjectId).as(PresentId) { presentId: PresentId =>
      (post & pathEndOrSingleSlash){
        entity(as[CommentOrActivateParameters]) { params: CommentOrActivateParameters =>
          authenticateAdminBySession { implicit adminUserSession: AdminUserSession =>
            authorizeByResource(presentId) {
              if (params.comment.isDefined) {
                authorizeByRole(AdminRoleId.Client) {
                  val comment = params.comment.get
                  complete(presentWriteActor.ask(PresentWriteActor.CommentPresent(presentId, CommentPresentRequest(comment))).mapTo[SuccessfulResponse])
                }
              } else if (params.status.isDefined) {
                val status = params.status.get
                complete(presentWriteActor.ask(PresentWriteActor.ActivatePresent(ChangePresentStatusRequest(status, params.gateway), presentId, routeContext)).mapTo[SuccessfulResponse])
              } else reject
            }
          }
        }
      } ~
      pathPrefix(adminComment) {
        get {
          authenticateAdminBySession { implicit s: AdminUserSession =>
            (authorizeByRole(AdminRoleId.Client) & authorizeByResource(presentId)) {
              complete( presentReadActor.ask(PresentReadActor.GetPresentComment(presentId)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      (delete & pathEndOrSingleSlash & authenticateBySession) { userSession: UserSession =>
          complete(presentWriteActor.ask(PresentWriteActor.DeleteUserPresent(presentId, userSession.userId)).mapTo[SuccessfulResponse])
      }
    } ~
    pathPrefix(code) {
      path(Segment).as(PresentCode) { code: PresentCode =>
        get {
          authenticateAdminBySession { implicit adminUserSession: AdminUserSession =>
            authorizeByResource(code) {
              complete((presentReadActor ? PresentReadActor.GetPresentByCode(code, routeContext)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    } ~
    pathPrefix(location) {
      path(Segment / "admsearch").as(LocationId) { locationId: LocationId =>
        (get & authenticateAdminBySession) { implicit s: AdminUserSession =>
          (authorizeByRole(AdminRoleId.Client) & authorizeByResource(locationId)) {
            parameters('name.as[String] ?, 'status.as[PresentStatusId] ?, 'sort.as[PresentAdminSearchSortType] ?) {
              (name: Option[String], status: Option[PresentStatusId], sort: Option[PresentAdminSearchSortType]) =>
                presentAdminSearchOffset { offsetLimit: OffsetLimit =>
                  complete((presentReadActor ? PresentReadActor.GetPresentsAdminSearch(PresentAdminSearchRequest(name, locationId, status, sort, offsetLimit)))
                    .mapTo[SuccessfulResponse])
                }
            }
          }
        }
      }
    }
  } ~
  (mainPrefix & pathPrefix(sentPresents)) { implicit routeContext: RouteContext =>
    (get & authenticateBySession & offsetLimit) { (userSession: UserSession, offsetLimit: OffsetLimit) =>
      parameters('type.as[PresentType] ?, 'status.as[PresentStatus] ?).as(SentPresentsRequest) { request: SentPresentsRequest =>
        complete((presentReadActor ? PresentReadActor.GetSentPresents(userSession.userId, request, offsetLimit, routeContext)).mapTo[SuccessfulResponse])
      }
    }
  }

}

object PresentService {
  val Root = PathMatcher("presents")
  val code = PathMatcher("code")
  val location = PathMatcher("location")
  val adminComment = PathMatcher("admcomment")
  val sentPresents = PathMatcher("sentpresents")
}

case class GetPresentParameters(presentType: Option[String], status: Option[String]) extends Validatable {

  val validTypes = List("product", "certificate")
  val validStatus = List("valid", "expired", "received")

  def validate(ctx: ValidationContext) {
    presentType.map { t =>
      if(!validTypes.contains(t)) ctx.fail(s"Type must be ${validTypes.mkString(",")} or not set")
    }
    status.map { s =>
      if(!validStatus.contains(s)) ctx.fail(s"Status must be ${validStatus.mkString(",")} or not set")
    }
  }

}

case class CommentOrActivateParameters(status: Option[String], gateway: Option[Gateway], @JsonProperty("admcomment") comment: Option[String]) extends Validatable with UnmarshallerEntity {

  def validate(ctx: ValidationContext) = {
    if (status.nonEmpty && comment.nonEmpty) {
      ctx.fail("Only status or comment must be set")
    } else if (status.isEmpty && comment.isEmpty){
      ctx.fail("Status or comment must be set")
    } else if (gateway.nonEmpty && comment.nonEmpty) {
      ctx.fail("Gateway can be set only with status")
    }
  }
}

case class Gateway(@JsonProperty("gatewayid") gatewayId: String, @JsonProperty("transactionid") transactionId: String) extends UnmarshallerEntity

case class SentPresentsRequest(presentType: Option[PresentType], presentStatus: Option[PresentStatus])