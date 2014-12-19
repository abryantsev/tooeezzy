package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import spray.routing.PathMatcher
import com.tooe.core.usecase.location_photoalbum.{LocationPhotoAlbumReadActor, LocationPhotoAlbumWriteActor}
import com.tooe.core.domain._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import spray.http.StatusCodes
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumReadActor.{GetLocationsPhotoByAlbum, GetLocationsChainPhotoAlbums}

class LocationPhotoAlbumService(implicit val system: ActorSystem) extends SprayServiceBaseClass2
  with ExecutionContextProvider
  with FileSystemHelper
{
  import LocationPhotoAlbumService._

  lazy val locationPhotoAlbumReadActor = lookup(LocationPhotoAlbumReadActor.Id)
  lazy val locationPhotoAlbumWriteActor = lookup(LocationPhotoAlbumWriteActor.Id)

  val photoOffsetLimit = parameters('photoslimit.as[Int] ?, 'photosoffset.as[Int] ?) as ((limit: Option[Int], offset: Option[Int]) => OffsetLimit(offset, limit))

  val route = (mainPrefix & pathPrefix(Root)) { implicit routeContext: RouteContext =>
    pathEndOrSingleSlash {
      post {
        entity(as[AddLocationPhotoAlbumRequest]) { request: AddLocationPhotoAlbumRequest =>
          authenticateAdminBySession { implicit s: AdminUserSession =>
            (authorizeByRole(AdminRoleId.Client) & authorizeByResource(request.locationId)) {
              complete(StatusCodes.Created, locationPhotoAlbumWriteActor.ask(LocationPhotoAlbumWriteActor.CreateLocationPhotoAlbum(request, routeContext)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    } ~
    pathPrefix(location) {
      path(Segment).as(LocationId) { locationId: LocationId =>
        (get & offsetLimit) { offsetLimit: OffsetLimit =>
          authenticateAnyUser {
            case s: UserSession =>
              complete((locationPhotoAlbumReadActor ? LocationPhotoAlbumReadActor.GetLocationsPhotoAlbumsByLocation(locationId, offsetLimit)).mapTo[SuccessfulResponse])
            case a: AdminUserSession =>
              implicit val session: AdminUserSession = a
              authorizeByRole(AdminRoleId.Moderator | AdminRoleId.Client) {
                complete((locationPhotoAlbumReadActor ? LocationPhotoAlbumReadActor.GetLocationsPhotoAlbumsByLocation(locationId, offsetLimit)).mapTo[SuccessfulResponse])
              }
          }
        }
      }
    } ~
    pathPrefix(ObjectId).as(LocationPhotoAlbumId) { locationPhotoAlbumId: LocationPhotoAlbumId =>
      pathEndOrSingleSlash {
        (get & photoOffsetLimit) { offsetLimit: OffsetLimit =>
          authenticateAnyUser { s: AnySession =>
            parameters('view.as[ViewType] ?) { viewType: Option[ViewType] =>
              s match {
                case s: UserSession =>
                  complete((locationPhotoAlbumReadActor ? LocationPhotoAlbumReadActor.GetLocationsPhotoAlbum(locationPhotoAlbumId, offsetLimit, viewType.getOrElse(ViewType.None))).mapTo[SuccessfulResponse])
                case a: AdminUserSession =>
                  implicit val session = a
                  authorizeByRole(AdminRoleId.Moderator | AdminRoleId.Client) {
                    complete((locationPhotoAlbumReadActor ? LocationPhotoAlbumReadActor.GetLocationsPhotoAlbum(locationPhotoAlbumId, offsetLimit, viewType.getOrElse(ViewType.None))).mapTo[SuccessfulResponse])
                  }
              }
            }
          }
        } ~
        post {
          entity(as[ChangeLocationPhotoAlbumRequest]) { request: ChangeLocationPhotoAlbumRequest =>
            authenticateAdminBySession {implicit s: AdminUserSession =>
              (authorizeByRole(AdminRoleId.Client) & authorizeByResource(locationPhotoAlbumId)){
                complete((locationPhotoAlbumWriteActor ? LocationPhotoAlbumWriteActor.ChangeLocationPhotoAlbum(locationPhotoAlbumId, request)).mapTo[SuccessfulResponse])
              }
            }
          }
        } ~
        delete {
          authenticateAdminBySession {implicit s: AdminUserSession =>
            (authorizeByRole(AdminRoleId.Client) & authorizeByResource(locationPhotoAlbumId)) {
              complete((locationPhotoAlbumWriteActor ? LocationPhotoAlbumWriteActor.DeleteLocationsPhotoAlbum(locationPhotoAlbumId)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      path(photos) {
        (get & photoOffsetLimit) { offsetLimit: OffsetLimit =>
          authenticateBySession { _ =>
            complete((locationPhotoAlbumReadActor ? GetLocationsPhotoByAlbum(locationPhotoAlbumId, offsetLimit)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    pathPrefix(locationsChain) {
      path(Segment).as(LocationsChainId) { locationsChainId: LocationsChainId =>
        (get & offsetLimit) { offsetLimit: OffsetLimit =>
          authenticateBySession { _ =>
            complete((locationPhotoAlbumReadActor ? GetLocationsChainPhotoAlbums(locationsChainId, offsetLimit)).mapTo[SuccessfulResponse])
          }
        }
      }
    }
  }

}

object LocationPhotoAlbumService {

  val Root = PathMatcher("locationalbums")
  val location = PathMatcher("location")
  val locationsChain = PathMatcher("locationschain")
  val photos = PathMatcher("photos")

}

case class AddLocationPhotoAlbumRequest(@JsonProperty("locationid") locationId: LocationId,
                                         name: String,
                                         description: Option[String],
                                         @JsonProperty("mainphoto") mainPhoto: LocationMainPhoto) extends UnmarshallerEntity

case class ChangeLocationPhotoAlbumRequest(name: Option[String],
                                           description: Unsetable[String],
                                           @JsonProperty("photoid") photoId: Option[LocationPhotoId]) extends UnmarshallerEntity