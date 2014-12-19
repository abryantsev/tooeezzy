package com.tooe.api.service

import akka.actor.ActorSystem
import spray.routing.{PathMatcher, Directives}
import akka.pattern.ask
import com.tooe.core.usecase.location.{LocationSearchViewType, LocationSearchSortType}
import com.tooe.core.usecase._
import spray.http.StatusCodes
import com.tooe.core.usecase.LocationReadActor._
import com.tooe.core.domain._
import com.tooe.core.usecase.GetLocationMoreInfoResponse
import com.tooe.core.usecase.LocationsSearchRequest
import com.tooe.core.domain.LocationId
import com.tooe.core.usecase.LocationReadActor.GetLocationsSearch
import com.tooe.core.usecase.GetFavoriteLocationsResponse
import com.tooe.core.usecase.LocationReadActor.GetFavoriteLocations
import com.tooe.core.usecase.LocationsSearchResponse
import com.tooe.core.usecase.LocationReadActor.GetLocationInfo
import com.tooe.core.usecase.GetLocationInfoRequest
import com.tooe.core.usecase.AddOwnCategoryRequest
import com.tooe.core.usecase.ChangeAdditionalCategoryRequest
import com.tooe.core.usecase.AddOwnCategoryResponse
import com.tooe.core.usecase.GetFavoriteLocationsRequest
import com.tooe.core.usecase.GetProductCategoriesResponse
import com.tooe.core.db.mongo.domain.AdminRole

class LocationService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import LocationService._

  lazy val locationReadActor = lookup(LocationReadActor.Id)
  lazy val locationWriteActor = lookup(LocationWriteActor.Id)
  lazy val favoriteStatsActor = lookup(FavoriteStatsActor.Id)

  val route =
      (mainPrefix & path(LocationsPath / Segment / "productcategories").as(LocationId)) {
        (routeContext: RouteContext, locationId: LocationId) =>
          (get & authenticateAnyUser) { s: AnySession =>
              s match {
                case s: UserSession =>
                  complete(locationReadActor.ask(LocationReadActor.GetProductCategoriesOfLocation(locationId, routeContext)).mapTo[GetProductCategoriesResponse])
                case s: AdminUserSession =>
                  implicit val session = s
                  authorizeByRole(AdminRoleId.Client) {
                    complete(locationReadActor.ask(LocationReadActor.GetProductCategoriesOfLocation(locationId, routeContext)).mapTo[GetProductCategoriesResponse])
                  }
              }
          }
      } ~
      (mainPrefix & path(LocationsPath / Segment / "productcategories").as(LocationId)) {
        (routeContext: RouteContext, locationId: LocationId) =>
          post {
            entity(as[AddOwnCategoryRequest]) { r: AddOwnCategoryRequest =>
              authenticateAdminBySession { implicit s: AdminUserSession =>
                (authorizeByRole(AdminRoleId.Client) & authorizeByResource(locationId)) {
                  complete(StatusCodes.Created,
                      locationWriteActor.ask(LocationWriteActor.AddOwnProductCategory(locationId, r, routeContext)).mapTo[AddOwnCategoryResponse])
                }
              }
            }
          }
      } ~
      (mainPrefix & path(LocationsPath / Segment / "productcategories" / Segment).as(ChangeAdditionalCategoryRequest)) {
        (rc: RouteContext, request: ChangeAdditionalCategoryRequest) =>
          post {
            entity(as[RenameAdditionalCategoryParameters]) { params: RenameAdditionalCategoryParameters =>
              authenticateAdminBySession { implicit s: AdminUserSession =>
                (authorizeByRole(AdminRoleId.Client) & authorizeByResource(request.locationId)) {
                  complete {
                    // todo why to divide new name from changeProductParams
                    locationWriteActor.ask(LocationWriteActor.ChangeOwnProductCategory(request, params, rc)).mapTo[SuccessfulResponse]
                  }
                }
              }
            }
          } ~
          delete {
            authenticateAdminBySession { implicit s: AdminUserSession =>
              (authorizeByRole(AdminRoleId.Client) & authorizeByResource(request.locationId)) {
                complete(locationWriteActor.ask(LocationWriteActor.RemoveOwnProductCategory(request)).mapTo[SuccessfulResponse])
              }
            }
          }
      } ~
      (mainPrefix & path(LocationsPath / Segment / "details").as(LocationId)) {
        (routeContext: RouteContext, locationId: LocationId) =>
          get {
            authenticateBySession { userSession: UserSession =>
              complete {
                locationReadActor.ask(LocationReadActor.GetLocationMoreInfo(locationId, routeContext)).mapTo[GetLocationMoreInfoResponse]
              }
            }
          }
      } ~
      (mainPrefix & path(LocationsPath / PathMatcher("search"))) { routeContext: RouteContext =>
        get {
          (parameters( 'category, 'radius ?, 'lon, 'lat, 'sort.as[LocationSearchSortType] ?,
              'entities.as[CSV[LocationSearchViewType]] ?) & offsetLimit).as(LocationsSearchRequest) { ( request: LocationsSearchRequest) =>
            authenticateBySession { userSession: UserSession =>
              complete(locationReadActor.ask(GetLocationsSearch(request, routeContext.lang)).mapTo[LocationsSearchResponse])
            }
          }
        }
      } ~
      (mainPrefix & (path(LocationsPath / Segment) & parameters('view.as[ShowType] ?, 'userslimit.as[Int] ? 5)).as(GetLocationInfoRequest)) {
        (routeContext: RouteContext, lp: GetLocationInfoRequest) =>
          get {
            if (lp.getView == ShowType.Adm)
              authenticateAdminBySession { implicit s: AdminUserSession =>
                authorizeByRole(AdminRoleId.Client | AdminRoleId.Moderator) {
                  complete((locationReadActor ? GetLocationAdminInfo(lp.locationId, s.adminUserId, lp.getView, lp.usersLimit, routeContext)).mapTo[SuccessfulResponse])
                }
              }
            else authenticateBySession { s: UserSession =>
              complete((locationReadActor ? GetLocationInfo(lp.locationId, s.userId, lp.getView, lp.usersLimit, routeContext)).mapTo[SuccessfulResponse])
            }
          }
      } ~
      (mainPrefix & (path(LocationsPath / PathMatcher("user") / Segment) &
        parameters('region, 'category, 'entities.as[CSV[LocationSearchViewType]] ?) & offsetLimit).as(GetFavoriteLocationsRequest)) {
        (routeContext: RouteContext, flr: GetFavoriteLocationsRequest) =>
          get {
            authenticateBySession { userSession: UserSession =>
              complete(locationReadActor.ask(GetFavoriteLocations(flr, routeContext.lang)).mapTo[GetFavoriteLocationsResponse])
            }
          }
      } ~
      (mainPrefix & path(LocationsPath / Segment / "user").as(LocationId)) {
        (routeContext: RouteContext, locationId: LocationId) =>
          post {
            authenticateBySession { userSession: UserSession => {
                complete(StatusCodes.Created,
                  locationWriteActor.ask(LocationWriteActor.AddLocationToFavorite(locationId, userSession.userId)).mapTo[SuccessfulResponse]
                )
              }
            }
          } ~
          delete {
            authenticateBySession { userSession: UserSession => {
                complete(locationWriteActor.ask(LocationWriteActor.RemoveLocationFromFavorites(locationId, userSession.userId)).mapTo[SuccessfulResponse])
              }
            }
          }
      } ~
      (mainPrefix & path(LocationsPath / Segment / "statistics").as(LocationId)) { (_: RouteContext, locationId: LocationId) =>
        complete((locationReadActor ? LocationReadActor.GetLocationStatistics(locationId)).mapTo[SuccessfulResponse])
      } ~
      (mainPrefix & (path(LocationsPath / "region" / Segment / "search") & parameters('category ?, 'name ?, 'userids.as[CSV[UserId]] ?, 'sort.as[NamePopularitySortType] ?, 'entities.as[CSV[LocationSearchViewType]] ?) & offsetLimit).as(GetLocationsForInvitationRequest)) {
        (routeContext: RouteContext, request: GetLocationsForInvitationRequest) =>
          get {
            authenticateBySession { case UserSession(userId, _) =>
              complete(locationReadActor.ask(LocationReadActor.GetLocationsForInvitation(request, userId, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
      } ~
      (mainPrefix & (path(LocationsPath / "locationschain" / Segment / "search") & parameters('region, 'haspromo ?, 'entities.as[CSV[LocationSearchViewType]] ?) & offsetLimit).as(GetLocationsByChainRequest)) {
        (routeContext: RouteContext, request: GetLocationsByChainRequest) =>
          get {
            authenticateBySession { case UserSession(userId, _) =>
              complete(locationReadActor.ask(LocationReadActor.GetLocationsByChain(request, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
      } ~
      (mainPrefix & (path(LocationsPath / "checkinsearch") & parameters('lon, 'lat, 'entities.as[CSV[LocationSearchViewType]] ?) & offsetLimit).as(GetLocationsForCheckinRequest)) {
        (routeContext: RouteContext, request: GetLocationsForCheckinRequest) =>
          get {
            authenticateBySession { case UserSession(userId, _) =>
              complete(locationReadActor.ask(LocationReadActor.GetLocationsForCheckin(request, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
      } ~
      (mainPrefix & path(LocationsPath / "user" / Segment / "statistics").as(UserId)) { (_: RouteContext, userId: UserId) =>
        get {
          authenticateBySession { case UserSession(currentUserId, _) =>
            complete(favoriteStatsActor.ask(FavoriteStatsActor.GetStatsByUserId(userId)).mapTo[SuccessfulResponse])
          }
        }
      } ~
      (mainPrefix & path(LocationsPath / "user" / Segment / "regions").as(UserId)) { (rc: RouteContext, userId: UserId) =>
        (get & parameter('country.as[String])) { country: String =>
            authenticateBySession {
              case UserSession(currentUserId, _) =>
                complete(favoriteStatsActor.ask(FavoriteStatsActor.GetFavoriteRegions(userId, CountryId(country), rc.lang)).mapTo[SuccessfulResponse])
            }
          }
      }

}


object LocationService extends Directives {
  val LocationPath = PathMatcher("location")
  val LocationsPath = PathMatcher("locations")
}