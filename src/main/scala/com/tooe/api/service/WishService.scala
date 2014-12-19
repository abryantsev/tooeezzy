package com.tooe.api.service

import akka.actor.{ActorSystem, ActorRef}
import akka.pattern.ask
import spray.http.StatusCodes
import com.tooe.core.domain.{LocationId, ViewTypeEx, WishId, UserId}
import spray.routing.PathMatcher
import com.tooe.core.usecase._
import com.tooe.core.usecase.WishReason
import com.tooe.core.usecase.WishReadActor.SelectCurrentUserWishes
import com.tooe.core.usecase.WishReadActor.SelectParams
import com.tooe.core.usecase.NewWishRequest
import com.tooe.core.usecase.WishReadActor.SelectWishes

class WishService(wishReadActor: ActorRef, wishWriteActor: ActorRef)(implicit system: ActorSystem) extends SprayServiceBaseClass2 {

  import WishService._

  implicit val EC = scala.concurrent.ExecutionContext.Implicits.global

  lazy val wishLikeWriteActor = lookup(WishLikeWriteActor.Id)
  lazy val wishLikeReadActor = lookup(WishLikeReadActor.Id)

  val route =
    (mainPrefix & path(Path.Root / PathMatcher("user") / Segment).as(UserId)) { (routeContext: RouteContext, userId: UserId) =>
      get {
        authenticateBySession { s: UserSession =>
          parameter('location.as[LocationId] ?) { locOpt: Option[LocationId] =>
            select(routeContext)(params => SelectWishes(userId, params, currentUserId = s.userId, locOpt))
          }
        }
      }
    } ~
    (mainPrefix & pathPrefix(Path.Root)){ routeContext: RouteContext =>
        pathPrefix(Segment).as(WishId) { (wishId: WishId) =>
          path(Path.likes){
            get { offsetLimit {(offsetLimit: OffsetLimit) =>
                authenticateBySession { userSession: UserSession =>
                  complete((wishLikeReadActor ? WishLikeReadActor.GetLikes(userSession.userId, wishId, offsetLimit)).mapTo[SuccessfulResponse])
                }
              }
            } ~
            post {
              authenticateBySession { userSession: UserSession =>
                complete(StatusCodes.Created, (wishLikeWriteActor ? WishLikeWriteActor.SaveLike(wishId, userSession.userId)).mapTo[SuccessfulResponse])
              }
            } ~
            delete {
              authenticateBySession { userSession: UserSession =>
                complete(StatusCodes.OK, (wishLikeWriteActor ? WishLikeWriteActor.DislikeWish(wishId, userSession.userId)).mapTo[SuccessfulResponse])
              }
            }
          }
        }
    } ~
    (mainPrefix & path(Path.Root / Segment).as(WishId)) { (routeContext: RouteContext, wishId: WishId) =>
      get { parameters('view.as[ViewTypeEx] ?) { viewType =>
        authenticateBySession { s: UserSession =>
          complete(wishReadActor.ask(WishReadActor.GetWish(wishId, viewType getOrElse ViewTypeEx.None, routeContext.lang, currentUserId = s.userId)).mapTo[OneWishResponse])
        }
      }} ~
      post {
        entity(as[WishReason]) { wishReason: WishReason =>
          authenticateBySession { case UserSession(userId, _) =>
            complete(wishWriteActor.ask(WishWriteActor.UpdateWish(wishId, wishReason, userId)).mapTo[SuccessfulResponse])
          }
        }
      } ~
      delete {
        authenticateBySession { case UserSession(userId, _) =>
          complete(wishWriteActor.ask(WishWriteActor.DeleteWish(wishId, userId)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & pathSuffix(Path.Root)) { routeContext: RouteContext =>
      post {
        entity(as[NewWishRequest]) { newWish: NewWishRequest =>
          authenticateBySession { case UserSession(userId, _) =>
            complete(StatusCodes.Created, wishWriteActor.ask(WishWriteActor.MakeWish(newWish, userId)).mapTo[WishCreated])
          }
        }
      } ~
      get {
        authenticateBySession { case UserSession(userId, _) =>
          select(routeContext)(params => SelectCurrentUserWishes(userId, params))
        }
      }
    }

  private def select(routeContext: RouteContext)(messageFactory: SelectParams => Any) =
    (parameters('fullfilled.as[Boolean] ?, 'view.as[ViewTypeEx] ?) & offsetLimit) {
      (fulfilledOnly: Option[Boolean], viewType: Option[ViewTypeEx], offsetLimit: OffsetLimit) =>
        val params = SelectParams(
          fulfilledOnly = fulfilledOnly getOrElse false,
          viewType = viewType getOrElse ViewTypeEx.None,
          offsetLimit,
          routeContext.lang
        )
        val message = messageFactory(params)
        complete(wishReadActor.ask(message).mapTo[SuccessfulResponse])
  }
}

object WishService {
  object Path {
    val Root = PathMatcher("wishes")
    val likes = PathMatcher("likes")
  }
}