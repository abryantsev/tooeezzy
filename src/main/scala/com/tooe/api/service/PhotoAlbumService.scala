package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import spray.routing.{Directives, PathMatcher}
import com.tooe.core.domain.{ViewType, PhotoId, PhotoAlbumId, UserId}
import spray.http.StatusCodes
import com.tooe.core.usecase._
import scala.concurrent.Future
import com.tooe.core.exceptions.ApplicationException

class PhotoAlbumService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with FileSystemHelper with PhotoOffsetLimitHelper with DigitalSignHelper with SettingsHelper {

  import PhotoAlbumService._

  lazy val photoAlbumWriteActor = lookup(PhotoAlbumWriteActor.Id)
  lazy val photoAlbumReadActor = lookup(PhotoAlbumReadActor.Id)

  override implicit val timeout = Timeout(10, TimeUnit.MINUTES)
  implicit val ec = scala.concurrent.ExecutionContext.global

  val route =
    (mainPrefix & path(photos)) { implicit routeContext: RouteContext =>
      post {
        optionalDigitalSign {  dsign: DigitalSign =>
          entity(as[AddPhotoAlbumRequest]) { request: AddPhotoAlbumRequest =>
            authenticateBySession { userSession: UserSession =>
              checkDigitalSign(Option(dsign).filter(_ => request.mainphoto.value.isEmpty)) {
                case Right(_) =>
                  request.mainphoto.value.map {
                    photoValue =>
                      storeFile(photoValue) {
                        fileName: String => {
                          val photo = request.mainphoto
                          addRequestComplete(request, routeContext, userSession,
                            AddPhotoParams(fileName, photo.toPhotoFormat, name = photo.name))
                        }
                      }
                  } getOrElse {
                    addRequestComplete(request, routeContext, userSession, AddPhotoParams(request.mainphoto.url.getOrElse(""), None, name = request.mainphoto.name))
                  }

                case Left(message) => complete(Future.failed[SuccessfulResponse](ApplicationException(message = message)))
              }
            }
          }
        }
      } ~
      (get & offsetLimit) { offsetLimit: OffsetLimit =>
        authenticateBySession { userSession: UserSession =>
          complete(photoAlbumReadActor.ask(PhotoAlbumReadActor.GetPhotoAlbumsByUser(userSession.userId, offsetLimit, userSession.userId)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    (mainPrefix & pathPrefix(photos)) { implicit routeContext: RouteContext =>
        pathPrefix(PathMatcher("user")) {
          path(Segment).as(UserId) { userId: UserId =>
            (get & offsetLimit) { offsetLimit: OffsetLimit =>
              authenticateBySession { userSession: UserSession =>
                complete(photoAlbumReadActor.ask(PhotoAlbumReadActor.GetPhotoAlbumsByUser(userId, offsetLimit, userSession.userId)).mapTo[SuccessfulResponse])
              }
            }
          }
        } ~
        pathPrefix(Segment).as(PhotoAlbumId) { albumId: PhotoAlbumId =>
          pathEndOrSingleSlash {
            delete {
              authenticateBySession { userSession: UserSession =>
                complete(photoAlbumWriteActor.ask(PhotoAlbumWriteActor.DeletePhotoAlbum(albumId, userSession.userId)).mapTo[SuccessfulResponse])
              }
            } ~
            post {
              entity(as[EditPhotoAlbumRequest]) { request: EditPhotoAlbumRequest =>
                authenticateBySession { userSession: UserSession =>
                  complete(photoAlbumWriteActor.ask(PhotoAlbumWriteActor.EditPhotoAlbum(request, userSession.userId, albumId)).mapTo[SuccessfulResponse])
                }
              }
            } ~
            (get & photoOffsetLimit) { offsetLimit: OffsetLimit =>
              authenticateBySession { userSession: UserSession =>
                parameters('view.as[ViewType] ?) { viewType: Option[ViewType] =>
                  complete(photoAlbumReadActor.ask(PhotoAlbumReadActor.GetPhotoAlbum(albumId, offsetLimit, userSession.userId, viewType.getOrElse(ViewType.None))).mapTo[SuccessfulResponse])
                }
              }
            }
          } ~
          path("photos") {
            authenticateBySession { userSession: UserSession =>
              (get & photoOffsetLimit) { offsetLimit: OffsetLimit =>
                complete(photoAlbumReadActor.ask(PhotoAlbumReadActor.GetPhotosByAlbum(albumId, offsetLimit, userSession.userId)).mapTo[SuccessfulResponse])
              }
            }
          }
        }
      }

  def addRequestComplete(request: AddPhotoAlbumRequest, routeContext: RouteContext, userSession: UserSession, addPhoto: AddPhotoParams) =
    complete(
      StatusCodes.Created,
      (photoAlbumWriteActor ? PhotoAlbumWriteActor.AddPhotoAlbum(
        userSession.userId,
        request.name,
        request.description,
        request.usergroups,
        addPhoto,
        routeContext)).mapTo[SuccessfulResponse]
    )

}

trait PhotoOffsetLimitHelper { self: Directives =>
  val photoOffsetLimit = parameters('photosoffset.as[Int] ?, 'photoslimit.as[Int] ?) as ((offset: Option[Int], limit: Option[Int]) => OffsetLimit(offset, limit))
}

case class UserGroups(view: Option[Seq[String]], comments: Option[Seq[String]]) extends UnmarshallerEntity

@deprecated("Separate logic to two scenarios: 1. photo has url, 2. photo has value")
case class AddPhotoAlbumRequest(name: Option[String], usergroups: Option[UserGroups], description: Option[String], mainphoto: Photo) extends UnmarshallerEntity

case class EditPhotoAlbumRequest(name: Option[String], usergroups: Option[UserGroups], description: Option[String], photoid: Option[PhotoId]) extends UnmarshallerEntity

object PhotoAlbumService {

  val photos = PathMatcher("photoalbums")

}
