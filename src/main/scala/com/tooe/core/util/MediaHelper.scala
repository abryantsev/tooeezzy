package com.tooe.core.util

import com.tooe.core.domain._
import com.tooe.core.main.SharedActorSystem
import com.tooe.extensions.scala.Settings
import com.tooe.core.domain.MediaUrl
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.db.mongo.domain.{UserMedia, User}
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.db.mongo.domain.UserMedia.CDNStatic

object MediaHelper {

  private lazy val settings = Settings(SharedActorSystem.sharedMainActorSystem)

  sealed trait DefaultUrlType {
    def id: String
  }

  case object LocationDefaultUrlType extends DefaultUrlType {
    def id = settings.DefaultImages.location
  }

  case object ProductDefaultUrlType extends DefaultUrlType {
    def id = settings.DefaultImages.product
  }

  case object PromotionDefaultUrlType extends DefaultUrlType {
    def id = settings.DefaultImages.promotion
  }

  case object CompanyDefaultUrlType extends DefaultUrlType {
    def id = settings.DefaultImages.company
  }

  case class UserDefaultUrlType(gender: Gender) extends DefaultUrlType {
    val id = gender match {
      case Gender.Female => settings.DefaultImages.femaleUser
      case Gender.Male => settings.DefaultImages.maleUser
    }
  }

  case object PhotoDefaultUrlType extends DefaultUrlType {
    def id = settings.DefaultImages.photo
  }

  case object UserBackgroundDefaultUrlType extends DefaultUrlType {
    def id = settings.DefaultImages.background
  }

  private lazy val s3Host = settings.S3.host

  private lazy val cdnHost = settings.CDN.host

  private lazy val staticCdnHost = settings.StaticCDN.host

  private def imageUrl(host: String, mediaObject: MediaObjectId, imageSize: String) =
    s"${host}$imageSize/${mediaObject.id}"

  private def defaultImageUrl(imageSize: String, defaultUrlType: DefaultUrlType) = staticUrl(defaultUrlType.id, imageSize)

  implicit class MediaObjectImplicits(val mo: MediaObject) extends AnyVal {

    def asUrl(imageSize: String): String = {
      mo.mediaType.getOrElse(UrlType.cdn) match {
        case UrlType.cdn => cdnUrl(mo.url, imageSize)
        case UrlType.s3 => imageUrl(s3Host, mo.url, imageSize)
        case UrlType.static => staticUrl(mo.url.id, imageSize)
        case UrlType.http if settings.HttpImages.showHttpImages => mo.url.id
        case _ => throw ApplicationException(message = "Invalid image URL")
      }
    }

    def asMediaUrl(imageSize: String) = MediaUrl(asUrl(imageSize))

    def asDefaultUrl(imageSize: String, defaultUrlType: DefaultUrlType) = defaultImageUrl(imageSize, defaultUrlType)

  }

  implicit class OptMediaObjectImplicits(val moOpt: Option[MediaObject]) extends AnyVal {

    def asUrl(imageSize: String, defaultUrlType: DefaultUrlType): String =
      moOpt.map(_.asUrl(imageSize)).getOrElse(defaultImageUrl(imageSize, defaultUrlType))

    def asMediaUrl(imageSize: String, defaultUrlType: DefaultUrlType): MediaUrl =
      moOpt.map(_.asMediaUrl(imageSize)).getOrElse(MediaUrl(defaultImageUrl(imageSize, defaultUrlType)))

  }

  implicit class UserMediaImplicits(val um: Seq[UserMedia]) extends AnyVal {
    private def avatarIsAbsent = um.filter(_.mediaType == "f").find(m => m.purpose == Some("main") | m.purpose == None) == None
    private def backgroundIsAbsent = um.find(m => m.purpose == Some("bg")) == None

    def withDefaultAvatar(imageSize: String, user: User) =
      if(avatarIsAbsent) um :+ UserMedia(MediaObject(UserDefaultUrlType(user.gender).id, Some(UrlType.static)), None, "f", Some("main"), cdnType = CDNStatic)
      else um

    def withDefaultBackground(imageSize: String) =
      if(backgroundIsAbsent) um :+ UserMedia(MediaObject(UserBackgroundDefaultUrlType.id, Some(UrlType.static)), None, "f", Some("bg"), cdnType = CDNStatic)
      else um
  }

  implicit class MediaItemDtoImplicits(val mid: MediaItemDto) extends AnyVal {

    def asMediaItem(imageSize: String): MediaItem = MediaItem(mid, imageSize)

  }

  implicit class OptMediaItemDtoImplicits(val optMid: Option[MediaItemDto]) extends AnyVal {

    def asMediaItem(imageSize: String, defaultUrlType: DefaultUrlType): MediaItem = // todo refactoring media item constructor
      optMid.map(_.asMediaItem(imageSize)).getOrElse(new MediaItem(null, defaultImageUrl(imageSize, defaultUrlType)))
  }

  def cdnUrl(imageObject: MediaObjectId, imageSize: String) = imageUrl(cdnHost, imageObject, imageSize)

  def staticUrl(imageName: String, imageSize: String) = s"${staticCdnHost}$imageSize/$imageName"

  def staticMediaUrl(imageName: String, imageSize: String) = MediaUrl(staticUrl(imageName, imageSize))

}