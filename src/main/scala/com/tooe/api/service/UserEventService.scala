package com.tooe.api.service

import com.tooe.core.usecase._
import akka.pattern.ask
import akka.actor.ActorSystem
import spray.http.StatusCodes
import com.tooe.core.domain.UserEventId

class UserEventService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  lazy val userEventReadActor = lookup(UserEventReadActor.Id)
  lazy val userEventWriteActor = lookup(UserEventWriteActor.Id)

  val route =
    (mainPrefix & post & path("promoinvitations" / Segment).as(UserEventId)) { (routeContext: RouteContext, userEventId: UserEventId) =>
      entity(as[UserEventStatusUpdateRequest]) { request: UserEventStatusUpdateRequest =>
        authenticateBySession { s: UserSession =>
          complete {
            (userEventWriteActor ? UserEventWriteActor.PromotionInvitationReply(request, userEventId, s.userId)).mapTo[SuccessfulResponse]
          }
        }
      }
    } ~
    (mainPrefix & post & path("promoinvitations")) { routeContext: RouteContext =>
      entity(as[PromotionInvitationRequest]) { request: PromotionInvitationRequest =>
        authenticateBySession { s: UserSession =>
          def future = (userEventWriteActor ? UserEventWriteActor.NewPromotionInvitation(request, s.userId)).mapTo[SuccessfulResponse]
          complete(StatusCodes.Created, future)
        }
      }
    } ~
    (mainPrefix & path("userevents") & offsetLimit) { (routeContext: RouteContext, offsetLimit: OffsetLimit) =>
      get {
        authenticateBySession { s: UserSession =>
          complete((userEventReadActor ? UserEventReadActor.GetUserEvents(s.userId, offsetLimit, routeContext.lang)).mapTo[UserEvents])
        }
      } ~
      delete {
        authenticateBySession { s: UserSession =>
          complete((userEventWriteActor ? UserEventWriteActor.DeleteUserEvents(s.userId)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path("userevents" / Segment).as(UserEventId)) { (rc: RouteContext, id: UserEventId) =>
      get {
        authenticateBySession { s: UserSession =>
          complete((userEventReadActor ? UserEventReadActor.GetUserEvent(id, s.userId, rc.lang)).mapTo[UserEvents])
        }
      } ~
      delete {
        authenticateBySession { s: UserSession =>
          complete((userEventWriteActor ? UserEventWriteActor.DeleteUserEvent(id, s.userId)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & post & path("invitations")) { routeContext: RouteContext =>
      entity(as[InvitationRequest]) { request: InvitationRequest =>
        authenticateBySession { s: UserSession =>
          complete(StatusCodes.Created, userEventWriteActor.ask(UserEventWriteActor.NewInvitation(request, s.userId, routeContext.lang)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path("invitations" / Segment).as(UserEventId) & post) { (_: RouteContext, userEventId: UserEventId) =>
      entity(as[UserEventStatusUpdateRequest]) { request: UserEventStatusUpdateRequest =>
        authenticateBySession { s: UserSession =>
          complete {
          (userEventWriteActor ? UserEventWriteActor.InvitationReply(request, userEventId, s.userId)).mapTo[SuccessfulResponse]
          }
        }
      }
    }
}