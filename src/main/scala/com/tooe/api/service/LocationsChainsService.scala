package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern._
import spray.routing.PathMatcher
import com.tooe.core.domain.{CountryId, LocationsChainId}
import com.tooe.core.usecase.locationschain.{AddLocationsToChainParams, LocationsChainWriteActor, LocationsChainReadActor}
import com.tooe.core.usecase.locationschain.LocationsChainReadActor.{GetChainRegions, GetLocationChain}
import com.tooe.core.usecase.locationschain.LocationsChainWriteActor.AddLocationsToChain

class LocationsChainsService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider{
  import LocationsChainsService._

  lazy val locationsChainReadActor = lookup(LocationsChainReadActor.Id)
  lazy val locationsChainWriteActor = lookup(LocationsChainWriteActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
      path(Segment).as(LocationsChainId) { locationsChainId: LocationsChainId =>
        authenticateBySession { _: UserSession =>
          complete((locationsChainReadActor ? GetLocationChain(locationsChainId, routeContext)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & pathPrefix(Root / Segment / "locations").as(LocationsChainId)) { (routeContext: RouteContext, locationsChainId: LocationsChainId) =>
      post {
        entity(as[AddLocationsToChainParams]) { params: AddLocationsToChainParams =>
          authenticateAdminBySession { _: AdminUserSession =>
            complete((locationsChainWriteActor ? AddLocationsToChain(locationsChainId, params.locations)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    (mainPrefix & path(Root / Segment / "statistics").as(LocationsChainId)) { (_: RouteContext, lcid: LocationsChainId)  =>
      get {
        authenticateBySession { case UserSession(userId, _) =>
          complete(locationsChainReadActor.ask(LocationsChainReadActor.GetLocationsChainStats(lcid)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & path(Root / Segment / "regions").as(LocationsChainId)) { (rc: RouteContext, location: LocationsChainId) =>
      (get & parameter('country.as[String])) { country: String =>
          authenticateBySession { case UserSession(userId, _) =>
            complete(locationsChainReadActor.ask(GetChainRegions(location, CountryId(country), rc)).mapTo[SuccessfulResponse])
          }
        }

    }
}

object LocationsChainsService {
  val Root = PathMatcher("locationschains")
}