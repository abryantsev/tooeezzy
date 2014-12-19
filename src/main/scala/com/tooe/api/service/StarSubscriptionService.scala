package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern._
import com.tooe.core.usecase.{StarSubscriptionRequest, StarSubscriptionActor}
import spray.routing.PathMatcher
import com.tooe.core.domain.UserId
import spray.http.StatusCodes
import com.tooe.core.usecase.StarSubscriptionActor.{GetUserOwnStarSubscription, GetStarSubscriptionUsers}

class StarSubscriptionService (implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import StarSubscriptionService._
  import scala.concurrent.ExecutionContext.Implicits.global


  lazy val starSubscriptionActor = lookup(StarSubscriptionActor.Id)

  val starSubscriptionsOffsetLimit = parameters('offset.as[Int] ?, 'limit.as[Int] ?) as ((offset: Option[Int], limit: Option[Int]) => OffsetLimit(offset, limit, 0, 20))

  val userStarSubscriptionsOffsetLimit = parameters('offset.as[Int] ?, 'limit.as[Int] ?) as ((offset: Option[Int], limit: Option[Int]) => OffsetLimit(offset, limit, 0, 32))

  val route =
    (mainPrefix & path(Root)) { (_: RouteContext) =>
      authenticateBySession { case UserSession(currentUserId, _) => {
        post {
            entity(as[StarSubscriptionRequest]) { (request: StarSubscriptionRequest) =>
              complete(StatusCodes.Created ,starSubscriptionActor.ask(StarSubscriptionActor.Subscribe(currentUserId, request.starId)).mapTo[SuccessfulResponse])
            }
          } ~
        (get & userStarSubscriptionsOffsetLimit) { offsetLimit: OffsetLimit =>
          complete((starSubscriptionActor ? GetUserOwnStarSubscription(currentUserId, offsetLimit)).mapTo[SuccessfulResponse])
        }
      }
      }
    } ~
    (mainPrefix & pathPrefix(Root / "star" / Segment).as(UserId)) { (_: RouteContext, userId: UserId) =>
      path("users") {
        (get & starSubscriptionsOffsetLimit) { offsetLimit: OffsetLimit =>
           complete((starSubscriptionActor ? GetStarSubscriptionUsers(userId, offsetLimit)).mapTo[SuccessfulResponse])
        }
      } ~
      pathEndOrSingleSlash {
        delete {
          authenticateBySession { case UserSession(currentUserId, _) =>
            complete(starSubscriptionActor.ask(StarSubscriptionActor.UnSubscribe(currentUserId, userId)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    (mainPrefix & path(Root / user / Segment).as(UserId)) { (_: RouteContext, userId: UserId) =>
        (get & userStarSubscriptionsOffsetLimit) { offsetLimit: OffsetLimit =>
          complete((starSubscriptionActor ? GetUserOwnStarSubscription(userId, offsetLimit)).mapTo[SuccessfulResponse])
      }
    }
}

object StarSubscriptionService {
  val Root = PathMatcher("starsubscriptions")
  val user = PathMatcher("user")
}
