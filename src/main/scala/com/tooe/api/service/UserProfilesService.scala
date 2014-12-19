package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern.ask
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.domain.{UserMedia, UserOnlineStatus}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.domain._
import com.tooe.core.usecase._
import com.tooe.core.usecase.user.UserMessageSettingUpdateItem
import java.util.Date
import spray.http.StatusCodes
import spray.routing.PathMatcher
import com.tooe.api.validation.{ValidationContext, Validatable}
import com.tooe.core.exceptions.ApplicationException

class UserProfilesService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with FileSystemHelper with ExecutionContextProvider with DigitalSignHelper with SettingsHelper {

  import UserProfilesService._

  implicit val EC = scala.concurrent.ExecutionContext.Implicits.global

  val userReadActor = lookup(UserReadActor.Id)
  val userWriteActor = lookup(UserWriteActor.Id)


  val route =  (mainPrefix & pathPrefix(Root))  { implicit routeContext: RouteContext =>
    authenticateBySession { userSession: UserSession => {
      pathEndOrSingleSlash {
        get {
          parameter('view.as[ViewType] ?) { viewType: Option[ViewType] =>
            complete((userReadActor ? UserReadActor.GetUserProfile(userSession.userId, viewType.getOrElse(ViewType.None), routeContext)).mapTo[SuccessfulResponse])
          }
        } ~
        post {
          optionalDigitalSign { dsign: DigitalSign =>
            entity(as[UserProfileUpdateRequest]) { request: UserProfileUpdateRequest =>
              complete((userWriteActor ? UserWriteActor.UpdateUserProfile(userSession.userId, saveMediaToFile(request), routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        } ~
        post {
          entity(as[SetMainAvatar]) { request: SetMainAvatar =>
            complete(StatusCodes.Created, (userWriteActor ? UserWriteActor.SetUserMainMedia(userSession.userId, request, routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      } ~
      path("usermedia") {
        post {
          entity(as[AddUserMediaRequest]) { request: AddUserMediaRequest =>
            complete(StatusCodes.Created, (userWriteActor ? UserWriteActor.AddUserMedia(userSession.userId, request, routeContext.lang, replaceMainAvatar = false)).mapTo[SuccessfulResponse])
          }
        } ~
        delete {
          entity(as[DeleteUserMediaRequest]) { request: DeleteUserMediaRequest =>
            complete((userWriteActor ? UserWriteActor.DeleteUserMedia(userSession.userId, request)).mapTo[SuccessfulResponse])
          }
        }
      }
    }
    }
  }

  def saveMediaToFile(request: UserProfileUpdateRequest): UserProfileUpdateRequest = {
    val newUserMedia = request.media map { media => {
      val photoValue = storeFileToTmpDirectory(media.value) getOrElse (
        throw ApplicationException(
          message = "Uploading file can't be stored on the server. See logs.",
          statusCode = StatusCodes.InternalServerError
        )
      )
      media.copy(value = photoValue)
    }}
    request.copy(media = newUserMedia)
  }
}

object UserProfilesService {
  val Root = PathMatcher("userprofiles")
}

case class UserProfileUpdateRequest (
  name: Option[String],
  @JsonProperty("lastname") lastName: Option[String],
  @JsonProperty("email") updatedEmail: Option[String],
  @JsonProperty("mainphone") mainPhone: Unsetable[PhoneShort],
  @JsonProperty("phones") additionalPhones: Unsetable[Seq[PhoneShort]],
  @JsonProperty("regionid") regionId: Option[RegionId],
  gender: Option[Gender],
  birthday: Unsetable[Date],
  media: Option[UserProfileMediaUpdateRequest],
  @JsonProperty("maritalstatus")maritalStatus: Unsetable[MaritalStatusId],
  education: Unsetable[String],
  job: Unsetable[String],
  @JsonProperty("aboutme")aboutMe: Unsetable[String],
  settings: Option[UserSettingsProfileItem],
  @JsonProperty("messagesettings")messageSettings: Option[UserMessageSettingUpdateItem],
  @JsonProperty("onlinestatus")onlineStatus: Unsetable[OnlineStatusId]
) extends UnmarshallerEntity with Validatable
{
  def email = updatedEmail.map(_.toLowerCase)

  def newOnlineStatus: Option[OnlineStatusId] = {
    import Unsetable._
    onlineStatus match {
      case Skip          => None
      case Update(value) => Some(value)
      case Unset         => Some(UserOnlineStatus.defaultOnlineStatus)
    }
  }

  def phones: Seq[PhoneShort] = mainPhone.toOption.toSeq ++ additionalPhones.toOption.getOrElse(Nil)

  override def validate(ctx: ValidationContext) = {
    import Unsetable._
    (mainPhone, additionalPhones) match {
      case (Unset, Update(_)) => ctx.fail("Deleting main phone and setting additional phones is not legitimate.")
      case (Update(mp), Update(ps)) if ps contains mp => ctx.fail("Main phone must NOT present among phones")
      case validCombination =>
    }
    val dups = phones groupBy identity collect { case (p, ps) if ps.size > 1 => p }
    if (dups.size > 0) ctx.fail("Phones contain duplicates: " + dups)
  }
}

case class UserProfileMediaUpdateRequest(
  @JsonProperty("mimetype") mimeType: String,
  encoding: String,

  /**
   * Attention! There is tricky logic behind the scene.
   * This field contains base64 encoded image data after it's been deserialized from user request.
   * In business logic layer this field contains local temporary file path that represents image data.
   * So, persisting of the image happens in REST service layer!
   * TODO basically there should be two separated data structures for those two cases mentioned above!
   */
  value: String
)

case class SetMainAvatar(media: SetMainAvatarUrl) extends UnmarshallerEntity
case class SetMainAvatarUrl(url: String)

case class AddUserMediaRequest
(
  url: String,
  purpose: Option[String],
  description: Option[String], 
  style: Option[String],
  color: Option[String]) extends UnmarshallerEntity {

  def newBackgroundUserMedia = UserMedia(
    url = MediaObject(url),
    mediaType = "f",
    purpose = Some("bg"),
    descriptionColor = color,
    descriptionStyle = style,
    description = description
  )
  def newAvatarUserMedia = UserMedia(url = MediaObject(url), mediaType = "f")
  def newMainAvatarUserMedia = newAvatarUserMedia.copy(purpose = Some("main"))
  
  def mediaUrlAlreadyExists(userMedia: Seq[UserMedia]) = userMedia.exists(_.url.url.id == url)
}

case class DeleteUserMediaRequest(url: String) extends UnmarshallerEntity