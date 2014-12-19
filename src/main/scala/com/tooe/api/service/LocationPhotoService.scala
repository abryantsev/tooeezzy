package com.tooe.api.service

import spray.routing.PathMatcher
import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.core.usecase.location_photo.{LocationPhotoReadActor, LocationPhotoWriteActor}
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import spray.http.StatusCodes
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.domain.LocationPhotoAlbumId
import com.tooe.core.domain.LocationPhotoId

class LocationPhotoService(implicit val system: ActorSystem) extends SprayServiceBaseClass2
  with FileSystemHelper
  with DigitalSignHelper
  with SettingsHelper
  with ExecutionContextProvider
{
  import LocationPhotoService._

  lazy val locationPhotoReadActor = lookup(LocationPhotoReadActor.Id)
  lazy val locationPhotoWriteActor = lookup(LocationPhotoWriteActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) { implicit routeContext: RouteContext =>
      pathEndOrSingleSlash {
        authenticateAdminBySession { implicit s: AdminUserSession =>
          authorizeByRole(AdminRoleId.Client) {
            post {
              entity(as[AddLocationPhotoRequest]) { request: AddLocationPhotoRequest => {
                authorizeByResource(request.locationAlbumId) {
                storeFile(request.value) { value =>
                  complete(StatusCodes.Created,
                    (locationPhotoWriteActor ? LocationPhotoWriteActor.CreatePhotoFromValue(request.cloneWithNewValue(value))).mapTo[SuccessfulResponse])
                }
                }
              }}
            } ~
            post {
              entity(as[AddLocationPhotoUrlRequest]) { request: AddLocationPhotoUrlRequest =>
                authorizeByResource(request.locationAlbumId) {
                complete(StatusCodes.Created,(locationPhotoWriteActor ? LocationPhotoWriteActor.CreatePhotoFromUrl(request)).mapTo[SuccessfulResponse])
                }
              }
            }
          }
        }
      } ~
      pathPrefix(Segment).as(LocationPhotoId) { locationPhotoId: LocationPhotoId =>
        pathEndOrSingleSlash {
          get {
            authenticateBySession { userSession: UserSession =>
              parameter('view.as[ViewType] ?) { viewType: Option[ViewType] =>
                complete((locationPhotoReadActor ? LocationPhotoReadActor.ShowLocationPhoto(locationPhotoId, userSession.userId, viewType.getOrElse(ViewType.None))).mapTo[SuccessfulResponse])
              }
            }
          } ~
          post {
            authenticateAdminUser { implicit s: AdminUserSession =>
              entity(as[ChangeLocationPhotoRequest]) { request: ChangeLocationPhotoRequest =>
                (authorizeByRole(AdminRoleId.Client) | authorizeByResource(locationPhotoId)) {
                  complete((locationPhotoWriteActor ? LocationPhotoWriteActor.ChangeLocationPhoto(locationPhotoId, request)).mapTo[SuccessfulResponse])
                }
              }
            }
          } ~
          delete {
            authenticateAdminUser { implicit s: AdminUserSession =>
              (authorizeByRole(AdminRoleId.Client) | authorizeByResource(locationPhotoId)) {
                complete((locationPhotoWriteActor ? LocationPhotoWriteActor.DeleteLocationPhoto(locationPhotoId)).mapTo[SuccessfulResponse])
              }
            }
          }
        } ~
        path(likes) {
          authenticateBySession { userSession: UserSession =>
            (get & offsetLimit) { offsetLimit: OffsetLimit =>
              complete((locationPhotoReadActor ? LocationPhotoReadActor.GetPhotoLikes(locationPhotoId, userSession.userId, offsetLimit)).mapTo[SuccessfulResponse])
            } ~
            post {
              complete(StatusCodes.Created, (locationPhotoWriteActor ? LocationPhotoWriteActor.LikeLocationPhoto(locationPhotoId, userSession.userId)).mapTo[SuccessfulResponse])
            } ~
            delete {
              complete((locationPhotoWriteActor ? LocationPhotoWriteActor.UnlikeLocationPhoto(locationPhotoId, userSession.userId)).mapTo[SuccessfulResponse])
            }
          }
        } ~
        path(comments) {
          authenticateBySession { userSession: UserSession =>
            (get & offsetLimit) {  offsetLimit: OffsetLimit =>
              complete((locationPhotoReadActor ? LocationPhotoReadActor.GetLocationPhotoComments(locationPhotoId, userSession.userId, offsetLimit)).mapTo[SuccessfulResponse])
            } ~
            post {
              entity(as[PhotoMessage]) { comment: PhotoMessage =>
                optionalDigitalSign {  dsign: DigitalSign =>
                  val future = (locationPhotoWriteActor ? LocationPhotoWriteActor.AddLocationPhotoComment(locationPhotoId, userSession.userId, comment, dsign, routeContext.lang)).mapTo[SuccessfulResponse]
                  complete(StatusCodes.Created, future)
                }
              }
            }
          }
        }
      }
  }

}

object LocationPhotoService {
  val Root = PathMatcher("locationphotos")
  val likes = PathMatcher("likes")
  val comments = PathMatcher("comments")
}

case class ChangeLocationPhotoRequest(name: Unsetable[String]) extends UnmarshallerEntity

case class AddLocationPhotoRequest(mimetype: String,
                                   encoding: String,
                                   value: String,
                                   name: Option[String],
                                   @JsonProperty("locationalbumid") locationAlbumId: LocationPhotoAlbumId) extends UnmarshallerEntity {

  def cloneWithNewValue(newValue: String) =
    AddLocationPhotoRequest(mimetype, encoding, newValue, name, locationAlbumId)

}

case class AddLocationPhotoUrlRequest(url: String,
                                      name: Option[String],
                                      @JsonProperty("locationalbumid") locationAlbumId: LocationPhotoAlbumId,
                                      photoAlbum: Option[LocationPhotoAlbum]) extends UnmarshallerEntity