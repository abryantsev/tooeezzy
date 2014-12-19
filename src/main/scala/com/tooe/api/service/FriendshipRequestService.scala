package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase._
import spray.http.StatusCodes
import com.tooe.core.domain.FriendshipRequestId
import com.tooe.core.exceptions.ApplicationException

class FriendshipRequestService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import FriendshipRequestWriteActor._
  import FriendshipRequestReadActor._

  lazy val friendshipRequestWriteActor = lookup(FriendshipRequestWriteActor.Id)
  lazy val friendshipRequestReadActor = lookup(FriendshipRequestReadActor.Id)

  val route =
    (mainPrefix & path("friendshiprequests" / Segment).as(FriendshipRequestId)) { (_: RouteContext, friendshipRequestId: FriendshipRequestId) =>
      (post & entity(as[AcceptOrRejectFriendshipRequest])) { request: AcceptOrRejectFriendshipRequest =>
        authenticateBySession { s: UserSession =>
          complete {
            (friendshipRequestWriteActor ? AcceptOrRejectFriendship(request, friendshipRequestId, s.userId)).mapTo[SuccessfulResponse]
          }
        }
      }
    } ~
    (mainPrefix & path("friendshiprequests")) { routeContext: RouteContext =>
      post {
        entity(as[FriendshipInvitationRequest]) { request: FriendshipInvitationRequest =>
          authenticateBySession {
            case UserSession(request.userId, _) => throw ApplicationException(message = "Invalid user id")
            case UserSession(userId, _) =>
              complete(StatusCodes.Created, friendshipRequestWriteActor.ask(OfferFriendship(request.userId, userId, routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      } ~
      (get & offsetLimit) { offsetLimit: OffsetLimit =>
        authenticateBySession { s: UserSession =>
          complete {
            (friendshipRequestReadActor ? IncomingFriendshipRequests(s.userId, offsetLimit)).mapTo[SuccessfulResponse]
          }
        }
      }
    }
}