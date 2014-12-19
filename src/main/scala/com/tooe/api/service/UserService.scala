package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase._
import com.tooe.core.usecase.UserReadActor
import spray.routing.PathMatcher
import com.tooe.core.domain._
import com.tooe.api.validation.ValidationHelper
import com.tooe.core.usecase.UserReadActor.GetUserInfo
import com.tooe.core.usecase.NewsWriteActor.LeaveComment
import com.tooe.core.usecase.UserReadActor.GetUserMoreInfo

class UserService(implicit val system: ActorSystem) extends SprayServiceBaseClass2{

  import UserService._
  import scala.concurrent.ExecutionContext.Implicits.global

  val userSearchParams = (parameters('name ?, 'country ?, 'region ?, 'gender ?, 'maritalstatus ?, 'entities.as[CSV[UserFields]] ?)& offsetLimit).as(SearchUsersRequest)

  val userReadActor = lookup(UserReadActor.Id)
  val userCommentActor = lookup(NewsWriteActor.Id)

  val route =
    (mainPrefix & path(UsersPath / "search") & userSearchParams ) {
      (routeContext: RouteContext, request: SearchUsersRequest) => get {
        authenticateBySession { s: UserSession =>
          ValidationHelper.checkObject(request)
          complete( userReadActor.ask(UserReadActor.SearchUsers(request)).mapTo[SearchUsersResponse])
        }
      }
    } ~
    (mainPrefix & (path(UsersPath) & parameters('view.as[ViewTypeEx] ?)).as(GetUsersMainScreenRequest)) { (routeContext: RouteContext, r: GetUsersMainScreenRequest) =>
      get {
        authenticateBySession { case UserSession(userId, _) =>
          complete( userReadActor.ask(UserReadActor.GetUsersMainScreen(r, userId, routeContext)).mapTo[GetUsersMainScreenResponse])
        }
      }
    } ~
    (mainPrefix & (path(UsersPath / ObjectId).as(UserId) & parameters('view.as[ViewTypeEx] ?)).as(GetUserInfoRequest)) { (routeContext: RouteContext, r: GetUserInfoRequest) =>
      get {
        authenticateBySession { case UserSession(userId, _) =>
          complete( userReadActor.ask(GetUserInfo(r, userId, routeContext.lang)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path(UsersPath / Segment / "details").as(UserId)) { (routeContext: RouteContext, userId: UserId) =>
      get {
        authenticateBySession { case _ =>
          complete( userReadActor.ask(GetUserMoreInfo(userId)).mapTo[GetUserMoreInfoResponse])
        }
      }
    } ~
    (mainPrefix & path(UsersPath / Segment / "mutualfriends").as(UserId)) { (routeContext: RouteContext, userId: UserId) =>
      get {
        authenticateBySession { case UserSession(currentUserId, _) =>
          complete( userReadActor.ask(UserReadActor.GetMutualFriends(userId, currentUserId)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path(UsersPath / "statistics") & parameters('statistics.as[CSV[UsersOwnStatisticsViewType]] ?).as(GetUsersOwnStatisticsRequest)) { (routeContext: RouteContext, request: GetUsersOwnStatisticsRequest) =>
      get {
        authenticateBySession { case UserSession(userId, _) =>
          complete( userReadActor.ask(UserReadActor.GetOwnStatistics(request, userId, routeContext.lang)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & (path(UsersPath / Segment / "statistics") & parameters('statistics.as[CSV[UserStatisticViewType]] ?)).as(GetOtherUserStatisticsRequest)) { (routeContext: RouteContext, request: GetOtherUserStatisticsRequest) =>
      get {
        authenticateBySession { case UserSession(userId, _) =>
          complete( userReadActor.ask(UserReadActor.GetOtherUserStatistics(request, routeContext.lang)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path(UsersPath / "onlinestatus") & parameters('userids.as[CSV[UserId]] ?).as(GetUsersOnlineStatusesRequest)) { (routeContext: RouteContext, request: GetUsersOnlineStatusesRequest) => {
        ValidationHelper.checkObject(request)
        get {
          authenticateBySession { case UserSession(userId, _) =>
            complete( userReadActor.ask(UserReadActor.GetUsersOnlineStatuses(request.userIds, userId, routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
    }
}

object UserService {
  val UsersPath = PathMatcher("users")
}

