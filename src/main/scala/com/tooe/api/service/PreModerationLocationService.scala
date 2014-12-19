package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase.PreModerationLocationReadActor
import com.tooe.core.domain._
import com.tooe.core.usecase.location.PreModerationLocationAdminSearchSortType
import com.tooe.core.usecase.SearchPreModerationLocationsRequest
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.PreModerationLocationId

class PreModerationLocationService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import PreModerationLocationService._

  lazy val preModerationLocationReadActor = lookup(PreModerationLocationReadActor.Id)

  val adminSearchOffsetLimit = getOffsetLimit(defaultLimit = 10)

  val route = (mainPrefix & pathPrefix(Root)) {
    routeContext: RouteContext =>
      path(ObjectId).as(PreModerationLocationId) { pmlId: PreModerationLocationId =>
        get {
          authenticateAdminBySession { implicit s: AdminUserSession =>
            authorizeByResource(pmlId) {
              complete(preModerationLocationReadActor.ask(PreModerationLocationReadActor.GetPreModerationLocationById(pmlId, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      path("locations" / Segment).as(LocationId) { lId: LocationId =>
        get {
          authenticateAdminBySession { implicit s: AdminUserSession =>
            authorizeByResource(lId) {
              complete(preModerationLocationReadActor.ask(PreModerationLocationReadActor.GetPreModerationLocationByLocationId(lId, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      path("admsearch") {
        get {
          authenticateAdminBySession {
            implicit s: AdminUserSession =>
              authorizeByRole(AdminRoleId.Moderator) {
                adminSearchOffsetLimit { offsetLimit: OffsetLimit =>
                  parameters('name.as[String] ?, 'company.as[CompanyId] ?, 'modstatus.as[ModerationStatusId] ?, 'sort.as[PreModerationLocationAdminSearchSortType] ?).as(SearchPreModerationLocationsRequest) {
                    request: SearchPreModerationLocationsRequest  =>
                      complete(preModerationLocationReadActor.ask(PreModerationLocationReadActor.SearchPreModerationLocations(request, offsetLimit, routeContext.lang)).mapTo[SuccessfulResponse])
                  }
                }
              }
          }
        }
      }
  }
}

object PreModerationLocationService {
  val Root = "locationsmoderation"
}
