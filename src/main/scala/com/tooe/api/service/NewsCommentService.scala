package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase.{NewsCommentReadActor, NewsCommentWriteActor, SaveNewsCommentRequest, UpdateNewsCommentRequest}
import spray.routing.{PathMatcher, Directives}
import com.tooe.core.domain.{NewsCommentId, NewsId}
import spray.http.StatusCodes

class NewsCommentService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with DigitalSignHelper with SettingsHelper {

  import NewsCommentService._

  lazy val newsWriteActor = lookup(NewsCommentWriteActor.Id)
  lazy val newsReadActor = lookup(NewsCommentReadActor.Id)
  implicit val ec = scala.concurrent.ExecutionContext.global

  val newsOffsetLimit = getOffsetLimit(defaultLimit = 15)

  val route =
    (mainPrefix & path(NewsPath / Segment / CommentsPath).as(NewsId)) {
      (rc: RouteContext, newsId: NewsId) =>
        authenticateBySession { s: UserSession =>
          post {
            optionalDigitalSign {  dsign: DigitalSign =>
              entity(as[SaveNewsCommentRequest]) { sncr: SaveNewsCommentRequest =>
                  complete(StatusCodes.Created, newsWriteActor.ask(NewsCommentWriteActor.SaveComment(sncr, newsId, s.userId, dsign, rc.lang)).mapTo[SuccessfulResponse])
              }
            }
          } ~
          (get & newsOffsetLimit) { offsetLimit: OffsetLimit =>
              complete(newsReadActor.ask(NewsCommentReadActor.GetAllNewsCommentsByNewsId(newsId, offsetLimit)).mapTo[SuccessfulResponse])
          }
        }
    } ~
    (mainPrefix & path(NewsPath / CommentsPath / Segment).as(NewsCommentId)) {
      (rc: RouteContext, newsCommentId: NewsCommentId) =>
        authenticateBySession { s: UserSession =>
          delete {
             complete(newsWriteActor.ask(NewsCommentWriteActor.DeleteComment(newsCommentId, s.userId)).mapTo[SuccessfulResponse])
          } ~
          post {
            optionalDigitalSign {  dsign: DigitalSign =>
              entity(as[UpdateNewsCommentRequest]) { request: UpdateNewsCommentRequest =>
                complete(newsWriteActor.ask(NewsCommentWriteActor.UpdateComment(request, newsCommentId, s.userId, dsign, rc.lang)).mapTo[SuccessfulResponse])
              }
            }
          }
        }
    }
}

object NewsCommentService extends Directives {
  val NewsPath = PathMatcher("news")
  val CommentsPath = PathMatcher("comments")
}
