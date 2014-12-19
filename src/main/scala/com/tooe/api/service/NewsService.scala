package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase.NewsWriteActor
import spray.routing.{PathMatcher, Directives}
import com.tooe.core.usecase.news.{GetAllNewsRequest, NewsReadActor}
import com.tooe.core.domain.NewsId
import spray.http.StatusCodes
import com.tooe.core.usecase.news.NewsReadActor.NewsAnonymousActorSetting

class NewsService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider with DigitalSignHelper with SettingsHelper{

  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
  lazy val newsReadActor = lookup(NewsReadActor.Id)
  import NewsService._
  import com.tooe.core.domain.UserId
  import ApiVersions._

  val getNewsLikesOffsetLimit = getOffsetLimit(defaultLimit = 15)

  class VersionDependentParams(routeContext: RouteContext) {
    implicit val newsAnonymousActorSetting: NewsAnonymousActorSetting = {
      import NewsAnonymousActorSetting._
      if (routeContext.version == v01) WithoutActor
      else WithDefaultAvatar
    }
  }

  val route =
    (mainPrefix & path(NewsPath) & offsetLimit & parameters('usergroup ?).as(GetAllNewsRequest)) { (routeContext: RouteContext, offsetLimit: OffsetLimit, request: GetAllNewsRequest) =>
      import routeContext.lang
      val params = new VersionDependentParams(routeContext)
      import params._
      get {
        authenticateBySession { case UserSession(userId, _) =>
          complete((newsReadActor ? NewsReadActor.GetAllNews(request, userId, offsetLimit)).mapTo[SuccessfulResponse])

        }
      }
    } ~
    (mainPrefix & path(NewsPath / "user" / Segment).as(UserId) & offsetLimit) { (routeContext: RouteContext, userId: UserId, offsetLimit: OffsetLimit) =>
      import routeContext.lang
      val params = new VersionDependentParams(routeContext)
      import params._
      get {
        authenticateBySession { case UserSession(currentUserId, _) =>
          complete((newsReadActor ? NewsReadActor.GetAllNewsForUser(userId, currentUserId, offsetLimit)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path(NewsPath / Segment).as(NewsId)) { (routeContext: RouteContext, newsId: NewsId) =>
      import routeContext.lang
      val params = new VersionDependentParams(routeContext)
      import params._
      authenticateBySession { case UserSession(currentUserId, _) =>
        get {
            complete((newsReadActor ? NewsReadActor.GetNews(newsId, currentUserId)).mapTo[SuccessfulResponse])
        } ~
        delete {
          complete(newsWriteActor.ask(NewsWriteActor.HideNews(newsId, currentUserId)).mapTo[SuccessfulResponse])
        } ~
        post {
          complete(StatusCodes.Created, newsWriteActor.ask(NewsWriteActor.RestoreNews(newsId, currentUserId)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path(NewsPath / Segment / LikesPath).as(NewsId)) { (routeContext: RouteContext, newsId: NewsId) =>
      authenticateBySession { case UserSession(userId, _) =>
        post {
          complete(StatusCodes.Created, newsWriteActor.ask(NewsWriteActor.LikeNews(userId, newsId)).mapTo[SuccessfulResponse])
        } ~
        delete {
          complete(newsWriteActor.ask(NewsWriteActor.UnlikeNews(userId, newsId)).mapTo[SuccessfulResponse])
        } ~
        (get &  getNewsLikesOffsetLimit) { offsetLimit: OffsetLimit =>
          complete(newsReadActor.ask(NewsReadActor.GetNewsLikes(userId, newsId, offsetLimit)).mapTo[SuccessfulResponse])
        }
      }
    }
}

object NewsService extends Directives {
  val NewsPath = PathMatcher("news")
  val LikesPath = PathMatcher("likes")
}

