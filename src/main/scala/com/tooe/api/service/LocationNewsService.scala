package com.tooe.api.service

import spray.routing.PathMatcher
import akka.actor.ActorSystem
import akka.pattern.ask
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.domain._
import com.tooe.core.usecase.LocationNewsActor
import com.tooe.core.usecase.LocationNewsActor._
import spray.http.StatusCodes
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.db.mongo.domain.AdminRole

class LocationNewsService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationNewsService._

  lazy val locationNewsActor = lookup(LocationNewsActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) { implicit routeContext: RouteContext =>
      pathEndOrSingleSlash {
        post {
          entity(as[AddLocationNewsRequest]) { request: AddLocationNewsRequest =>
            authenticateAdminBySession { implicit s: AdminUserSession =>
              (authorizeByRole(AdminRoleId.Client) & authorizeByResource(request.locationId)) {
              complete(StatusCodes.Created,(locationNewsActor ? AddLocationNews(request, routeContext)).mapTo[SuccessfulResponse])
            }
            }
          }
        }
      } ~
      pathPrefix(Segment).as(LocationNewsId) { locationNewsId: LocationNewsId =>
        pathEndOrSingleSlash {
          post {
            entity(as[ChangeLocationNewsRequest]) { request: ChangeLocationNewsRequest =>
              authenticateAdminBySession { implicit s: AdminUserSession =>
                (authorizeByRole(AdminRoleId.Client) & authorizeByResource(locationNewsId)) {
                  complete((locationNewsActor ? ChangeLocationNews(locationNewsId, request, routeContext)).mapTo[SuccessfulResponse])
                }
              }
            }
          } ~
          delete {
            authenticateAdminBySession { implicit s: AdminUserSession =>
              (authorizeByRole(AdminRoleId.Client) & authorizeByResource(locationNewsId)) {
                complete((locationNewsActor ? DeleteLocationNews(locationNewsId)).mapTo[SuccessfulResponse])
              }
            }
          }
        } ~
        path("likes") {
          authenticateBySession { userSession: UserSession =>
            post {
              complete(StatusCodes.Created, (locationNewsActor ? LikeLocationNews(userSession.userId, locationNewsId)).mapTo[SuccessfulResponse])
            } ~
            delete {
              complete((locationNewsActor ? UnlikeLocationNews(userSession.userId, locationNewsId)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      path("locationschain" / Segment).as(LocationsChainId) { locationsChainId: LocationsChainId =>
        authenticateBySession { userSession: UserSession =>
          (get & offsetLimit) { offsetLimit: OffsetLimit =>
            complete((locationNewsActor ? GetLocationsChainNews(locationsChainId, userSession.userId, offsetLimit, routeContext)).mapTo[SuccessfulResponse])
          }
        }
      } ~
      path("location" / Segment).as(LocationId) { locationId: LocationId =>
        (get & offsetLimit) { offsetLimit: OffsetLimit =>
          parameters('view.as[ShowType] ?) { showTypeOpt: Option[ShowType] =>
            val showType = showTypeOpt getOrElse ShowType.None
            if (showType == ShowType.Adm) authenticateAdminBySession { implicit s: AdminUserSession =>
              authorizeByRole(AdminRoleId.Client) {
                val request = GetLocationNews(locationId, None, showType, offsetLimit, routeContext)
                complete((locationNewsActor ? request).mapTo[SuccessfulResponse])
              }
            }
            else authenticateBySession { s: UserSession =>
              def request = GetLocationNews(locationId, Some(s.userId), showType, offsetLimit, routeContext)
              complete((locationNewsActor ? request).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    }

}

object LocationNewsService {
  val Root = PathMatcher("locationnews")
}

case class AddLocationNewsRequest(@JsonProperty("locationid") locationId: LocationId,
                                  content: String,
                                  @JsonProperty("enablecomments") enableComments: Option[Boolean]) extends UnmarshallerEntity

case class ChangeLocationNewsRequest(content: Option[String],
                                     @JsonProperty("enablecomments") enableComments: Unsetable[Boolean]) extends UnmarshallerEntity
