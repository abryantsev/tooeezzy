package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import spray.routing.PathMatcher
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.domain.LocationId
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.LocationSubscriptionActor
import com.tooe.core.usecase.LocationSubscriptionActor._
import spray.http.StatusCodes

class LocationSubscriptionService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import LocationSubscriptionService._

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val locationSubscriptionActor = lookup(LocationSubscriptionActor.Id)

  val locationSubscriptionsOffsetLimit = offsetLimit()
  val getLocationSubscriptionsOffsetLimit = offsetLimit(defaultLimit = 32)

  val route = (mainPrefix & pathPrefix(Root)) { implicit routeContext: RouteContext =>
     pathEndOrSingleSlash {
      post {
        entity(as[LocationSubscriptionRequest]) { request: LocationSubscriptionRequest =>
          authenticateBySession { userSession: UserSession =>
            complete(StatusCodes.Created, (locationSubscriptionActor ? AddLocationSubscription(userSession.userId, request.locationId)).mapTo[SuccessfulResponse])
          }
        }
      } ~
      (get & getLocationSubscriptionsOffsetLimit) {  offsetLimit: OffsetLimit =>
        authenticateBySession { userSession: UserSession =>
          complete((locationSubscriptionActor ? GetUserLocationSubscriptions(userSession.userId, offsetLimit, routeContext.lang)).mapTo[SuccessfulResponse])
        }
      }
     } ~
      pathPrefix(location) {
        pathPrefix(Segment).as(LocationId) { locationId: LocationId =>
          path(users) {
            (get & locationSubscriptionsOffsetLimit) { offsetLimit: OffsetLimit =>
              complete((locationSubscriptionActor ? GetLocationSubscription(locationId, offsetLimit)).mapTo[SuccessfulResponse])
            }
          } ~
          pathEndOrSingleSlash {
            delete {
              authenticateBySession { userSession: UserSession =>
                complete((locationSubscriptionActor ? DeleteLocationSubscription(userSession.userId, locationId)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    }
  }

  def offsetLimit(defaultOffset:Int = 0, defaultLimit: Int = 20) =
    parameters('offset.as[Int] ?, 'limit.as[Int] ?) as ((offset: Option[Int], limit: Option[Int]) => OffsetLimit(offset, limit, defaultOffset, defaultLimit))

}

object LocationSubscriptionService {
  val Root = PathMatcher("locationsubscriptions")
  val location = PathMatcher("location")
  val users = PathMatcher("users")
}

case class LocationSubscriptionRequest(@JsonProperty("locationid") locationId: LocationId) extends UnmarshallerEntity