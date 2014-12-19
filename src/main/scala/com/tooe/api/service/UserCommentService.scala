package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase._
import spray.routing.{PathMatcher, Directives}
import com.tooe.core.domain.NewsId
import spray.http.StatusCodes
import com.tooe.core.usecase.news.NewsReadActor

class UserCommentService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with DigitalSignHelper with SettingsHelper{

  import UserCommentService._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
  lazy val newsReadActor = lookup(NewsReadActor.Id)

  val route =
    (mainPrefix & path(UserCommentsPath)) { (routeContext: RouteContext) =>
      post {
        entity(as[LeaveCommentRequest]) { ( request:LeaveCommentRequest) =>
          optionalDigitalSign {  dsign: DigitalSign =>
            authenticateBySession { case UserSession(userId, _) =>
              complete(StatusCodes.Created, newsWriteActor.ask(NewsWriteActor.LeaveComment(request, userId, dsign, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    } ~
    (mainPrefix & path(UserCommentsPath / Segment).as(NewsId)) { (routeContext: RouteContext, userCommentId: NewsId) =>
      post {
        entity(as[UpdateUserCommentRequest]) { ( request:UpdateUserCommentRequest) =>
          authenticateBySession { case UserSession(userId, _) =>
            complete(newsWriteActor.ask(NewsWriteActor.UpdateComment(request, userCommentId, userId)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    (mainPrefix & path(UserCommentsPath / Segment).as(NewsId)) { (routeContext: RouteContext, userCommentId: NewsId) =>
      get {
        authenticateBySession { case UserSession(userId, _) =>
          complete(newsReadActor.ask(NewsReadActor.GetUserCommentNews( userCommentId, userId, routeContext.lang)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path(UserCommentsPath / Segment).as(NewsId)) { (routeContext: RouteContext, userCommentId: NewsId) =>
      delete {
        authenticateBySession { case UserSession(userId, _) =>
          complete(newsWriteActor.ask(NewsWriteActor.RemoveComment( userCommentId, userId)).mapTo[SuccessfulResponse])
        }
      }
    }
}

object UserCommentService extends Directives {
  val UserCommentsPath = PathMatcher("usercomments")
}
