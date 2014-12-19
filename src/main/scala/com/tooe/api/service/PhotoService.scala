package com.tooe.api.service

import akka.actor.ActorSystem
import spray.routing.PathMatcher
import akka.pattern.ask
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.{AddPhotoParameters, PhotoReadActor, PhotoWriteActor}
import spray.http.StatusCodes
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import com.tooe.core.domain._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.validation.{Validatable, ValidationContext}
import scala.Some
import scala.concurrent.Future
import com.tooe.core.exceptions.ApplicationException

class PhotoService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with FileSystemHelper with DigitalSignHelper with SettingsHelper{

  import PhotoService._

  lazy val photoReadActor = lookup(PhotoReadActor.Id)
  lazy val photoWriteActor = lookup(PhotoWriteActor.Id)
  override implicit val timeout = Timeout(10, TimeUnit.MINUTES)
  implicit val ec = scala.concurrent.ExecutionContext.global

  val route =
    (mainPrefix & path(photos)) { implicit routeContext: RouteContext =>
        post {
          optionalDigitalSign {  dsign: DigitalSign =>
            entity(as[AddPhotoRequest]) { request: AddPhotoRequest =>
              authenticateBySession { userSession: UserSession =>
                checkDigitalSign(Option(dsign).filter(_ => request.value.isEmpty)) {
                  case Right(_) =>
                    request.value.map {
                      photoValue =>
                        storeFile(photoValue) {
                          fileName: String => {
                            addRequestComplete(userSession, AddPhotoParameters(request.toPhotoFormat, fileName, request.photoAlbumId, name = request.name), routeContext)
                          }
                        }
                    } getOrElse {
                      addRequestComplete(userSession, AddPhotoParameters(None, request.url.getOrElse(""), request.photoAlbumId, name = request.name), routeContext)
                    }
                  case Left(message) =>  complete(Future.failed[SuccessfulResponse](ApplicationException(message = message)))
                }
              }
            }
          }
        }
    } ~
      (mainPrefix & pathPrefix(photos)) {  routeContext: RouteContext =>
          pathPrefix(Segment).as(PhotoId) { (photoId: PhotoId) =>
                  pathEndOrSingleSlash {
                    delete {
                      authenticateBySession { userSession: UserSession =>
                        complete(photoWriteActor.ask(PhotoWriteActor.RemovePhoto(photoId, userSession.userId)).mapTo[SuccessfulResponse])
                      }
                    } ~
                      get {
                        parameter('view.as[ViewType] ?) { viewType: Option[ViewType] =>
                          authenticateBySession { userSession: UserSession =>
                            complete(photoReadActor.ask(PhotoReadActor.GetPhoto(photoId, userSession.userId, viewType.getOrElse(ViewType.None))).mapTo[SuccessfulResponse])
                          }
                        }
                      } ~
                    post {
                      entity(as[PhotoChangeRequest]) { request: PhotoChangeRequest =>
                        authenticateBySession { userSession: UserSession =>
                          complete(photoWriteActor.ask(PhotoWriteActor.ChangePhoto(photoId, request, userSession.userId)).mapTo[SuccessfulResponse])
                        }
                      }
                    }
                  } ~
                  path(likes) {
                    post {
                      authenticateBySession { userSession: UserSession =>
                        complete(StatusCodes.Created, (photoWriteActor ? PhotoWriteActor.LikePhoto(photoId, userSession.userId)).mapTo[SuccessfulResponse])
                      }
                    } ~
                      delete {
                        authenticateBySession { userSession: UserSession =>
                          complete((photoWriteActor ? PhotoWriteActor.DislikePhoto(photoId, userSession.userId)).mapTo[SuccessfulResponse])
                        }
                      }
                  } ~
                  path(comments) {
                    post {
                      entity(as[PhotoMessage]) { comment: PhotoMessage =>
                        optionalDigitalSign {  dsign: DigitalSign =>
                          authenticateBySession { userSession: UserSession =>
                            complete(StatusCodes.Created, (photoWriteActor ? PhotoWriteActor.CommentPhoto(photoId, userSession.userId, comment, dsign, routeContext.lang)).mapTo[SuccessfulResponse])
                          }
                        }
                      }
                    } ~
                    (get & offsetLimit) { offsetLimit: OffsetLimit =>
                      authenticateBySession { userSession: UserSession =>
                        complete((photoReadActor ? PhotoReadActor.GetPhotoComments(photoId, userSession.userId, offsetLimit)).mapTo[SuccessfulResponse])
                      }
                    }
                  }
              } ~
              path(comments / Segment).as(PhotoCommentId) { commentId: PhotoCommentId =>
                delete {
                  authenticateBySession { userSession: UserSession =>
                    complete((photoWriteActor ? PhotoWriteActor.DeletePhotoComment(userSession.userId, commentId)).mapTo[SuccessfulResponse])
                  }
                }
              }
          }

  def addRequestComplete(userSession: UserSession, photoParameters: AddPhotoParameters,routeContext: RouteContext) =
    complete(StatusCodes.Created, photoWriteActor.ask(PhotoWriteActor.AddPhoto(photoParameters, userSession.userId, routeContext)).mapTo[SuccessfulResponse])

}

object PhotoService {

  val photos = PathMatcher("photos")
  val likes = PathMatcher("likes")
  val comments = PathMatcher("comments")

}

//TODO very similar to com.tooe.api.service.Photo either one should be removed
@deprecated("Separate logic to two scenarios: 1. photo has url, 2. photo has value")
case class AddPhotoRequest(mimetype: Option[String],
                           encoding: Option[String],
                           value: Option[String],
                           name: Option[String],
                           url: Option[String],
                           @JsonProperty("photoalbumid") photoAlbumId: Option[PhotoAlbumId]) extends UnmarshallerEntity with Validatable {
  def validate(ctx: ValidationContext) {

    if (url.isDefined && (mimetype.isDefined || encoding.isDefined || value.isDefined))
      ctx.fail("Must be specified one of the url or the photo details")
    if (url.isEmpty && (mimetype.isEmpty || encoding.isEmpty || value.isEmpty))
      ctx.fail("Must be specified photo details: name, encoding, value and mimetype")
  }

  def toPhotoFormat: Option[PhotoFormat] =
    for (m <- mimetype; e <- encoding) yield PhotoFormat(mimetype = m, encoding = e)
}

case class PhotoChangeRequest(name: Unsetable[String]) extends UnmarshallerEntity

case class DigitalSign(signature: Option[String])