package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase._
import com.tooe.core.usecase.UsersCheckinSearchResponse
import com.tooe.core.domain.{CheckinViewType, LocationId, UserId}
import spray.routing.PathMatcher
import com.tooe.core.usecase.checkin.CheckinSearchSortType
import com.tooe.core.service.SearchNearParams

class CheckinService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import CheckinService._
  import CheckinReadActor._

  lazy val checkinActor = lookup(CheckinReadActor.Id)
  lazy val checkinWriteActor = lookup(CheckinWriteActor.Id)

  val route =
    (mainPrefix & path(CheckinsPath)) { routeContext: RouteContext =>
      post {
        entity(as[CheckinRequest]) { request: CheckinRequest =>
          authenticateBySession { userSession: UserSession =>
            complete((checkinWriteActor ? CheckinWriteActor.DoCheckin(request, userSession.userId, routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    (mainPrefix & path(CheckinsPath / PathMatcher("location")/Segment).as(LocationId)) {(routeContext: RouteContext, locationId: LocationId) =>
      (getOffsetLimit() & parameter('entities.as[CSV[CheckinViewType]] ?) ){ (offsetLimit: OffsetLimit, entitiesParams: Option[CSV[CheckinViewType]]) =>
        get{
          authenticateBySession { userSession: UserSession =>
            complete{
              def request = UsersCheckinRequest(locationId = locationId, entitiesParams = entitiesParams, offsetLimit = offsetLimit)
              checkinActor.ask(GetUsersCheckinsByLocation(request, userSession.userId)).mapTo[GenericCheckinUsersResponse]
            }
          }
        }
      }
    } ~
    (mainPrefix & path(CheckinsPath / PathMatcher("user") / Segment).as(UserId)) { (routeContext: RouteContext, userId: UserId) =>
      get {
        authenticateBySession { userSession: UserSession =>
          complete(checkinActor.ask(GetCheckinByUserId(userId)).mapTo[UsersCheckinSearchResponse])
        }
      }
    } ~
    (mainPrefix & path(CheckinsPath / PathMatcher("search"))) { routeContext: RouteContext =>
      get {
        (coordinates & parameter('sort.as[CheckinSearchSortType] ?) & offsetLimit) { (coords, sort, offsetLimit) =>
          authenticateBySession { userSession: UserSession =>
            def params = SearchNearParams(coords, offsetLimit, userSession.userId)
            def request = SearchNearRequest(params, sort)
            complete((checkinActor ? request).mapTo[CheckinsSearchResponse])
          }
        }
      }
    }
}

object CheckinService {
  val CheckinsPath = PathMatcher("checkins")
}