package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.domain.FriendField
import com.tooe.core.domain.UserId
import com.tooe.core.usecase._
import spray.routing._
import com.tooe.core.exceptions.ApplicationException

class FriendService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import FriendService._

  lazy val friendReadActor = lookup(FriendReadActor.Id)
  lazy val friendWriteActor = lookup(FriendWriteActor.Id)

  val friendsSearchOffsetLimit = getOffsetLimit()

  val route =
    (mainPrefix & path(FriendsPath / PathMatcher("user") / Segment / PathMatcher("online")).as(UserId)) { (routeContext: RouteContext, userId: UserId) =>
      (get & authenticateBySession & friendsOffsetLimit) { (s: UserSession, offsetLimit: OffsetLimit) =>
        parameters('entities.as[CSV[FriendField]] ?) { entities: Option[CSV[FriendField]] =>
          complete {
            val fields = entities.map(_.toSeq).getOrElse(FriendField.values).toSet
            if(fields.contains(FriendField.UsersCount) && offsetLimit.offset > 0)
              throw ApplicationException(message = "You cannot set offset > 0 and entity=userscount together")
            friendReadActor.ask(FriendReadActor.GetUserFriendsOnline(userId, offsetLimit, fields)).mapTo[SuccessfulResponse]
          }
        }
      }
    } ~
    (mainPrefix & path(FriendsPath / PathMatcher("search"))) { routeContext: RouteContext =>
      (parameters('name ?, 'country ?, 'usersgroup ?, 'entities.as[CSV[FriendField]] ?).as(SearchAmongOwnFriendsRequest) & friendsSearchOffsetLimit) {
        (sfr: SearchAmongOwnFriendsRequest, offsetLimit: OffsetLimit) =>
          (get & authenticateBySession) { s: UserSession =>
            complete {
              if(sfr.entities.getOrElse(FriendField.values.toSet).contains(FriendField.UsersCount) && offsetLimit.offset > 0)
                throw ApplicationException(message = "You cannot set offset > 0 and entity=userscount together")
              friendReadActor.ask(FriendReadActor.SearchAmongOwnFriends(sfr, s.userId, offsetLimit)).mapTo[SuccessfulResponse]
            }
          }
      }
    } ~
    handleRejections(searchUserFriendsRH) {
      (mainPrefix & path(FriendsPath / "user" / Segment / PathMatcher("search")).as(UserId)) { (routeContext: RouteContext, userId: UserId) =>
        (parameters('name ?, 'entities.as[CSV[FriendField]] ?).as(SearchUserFriendsRequest) & offsetLimit) {
          (request: SearchUserFriendsRequest, offsetLimit: OffsetLimit) =>
            (get & authenticateBySession) { s: UserSession =>
              complete {
                friendReadActor.ask(FriendReadActor.SearchUserFriends(request, userId, offsetLimit)).mapTo[SuccessfulResponse]
              }
            }
        }
      }
    } ~
    (mainPrefix & path(FriendsPath / Segment).as(UserId)) { (routeContext: RouteContext, userId: UserId) =>
        post {
          entity(as[UpdateFriendsGroupsRequest]) { request: UpdateFriendsGroupsRequest =>
            authenticateBySession { case UserSession(currentUserId, _) =>
                complete(friendWriteActor.ask(FriendWriteActor.UpdateFriendsGroups(userId, request.usergroups, currentUserId)).mapTo[SuccessfulResponse])
            }
          }
        } ~
        delete {
          authenticateBySession { case UserSession(currentUserId, _) =>
            complete(friendWriteActor.ask(FriendWriteActor.DeleteFriendship(userId, currentUserId)).mapTo[SuccessfulResponse])
          }
        }
    }
  
  def searchUserFriendsRH = RejectionHandler {
    case ValidationRejection(_,_) :: _ ⇒ complete( SearchFriendsResponse(users = Some(Seq()), usersCount = Some(0)))
  }

}

object FriendService extends Directives{
  val FriendsPath = PathMatcher("friends")

  val friendsOffsetLimit = parameters('offset.as[Int] ?, 'limit.as[Int] ?) as ((offset: Option[Int], limit: Option[Int]) => OffsetLimit(offset, limit, 0, 20))
}