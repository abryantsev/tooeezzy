package com.tooe.core.usecase.urls

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.core.domain._
import akka.actor.Actor
import org.bson.types.ObjectId
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.db.mongo.domain.Urls
import com.tooe.core.domain.PhotoAlbumId
import com.tooe.core.domain.PhotoId

object UrlsWriteActor {
  final val Id = Actors.UrlsWrite

  case class AddPhoto(id: PhotoId, url: MediaObjectId)
  case class AddPhotoAlbumFrontPhoto(id: PhotoAlbumId, url: MediaObjectId)
  case class AddUserMediaUrl(id: UserId, url: MediaObjectId)
  case class AddUserBackgroundUrl(id: UserId, url: MediaObjectId)
  case class AddLocationPhoto(id: LocationPhotoId, url: MediaObjectId)
  case class AddLocationPhotoAlbum(id: LocationPhotoAlbumId, url: MediaObjectId)
  case class AddProductMedia(id: ProductId, urls: Seq[MediaObjectId])
  case class AddPresentUrl(id: PresentId, url: MediaObjectId)
  case class AddModerationCompanyUrl(id: PreModerationCompanyId, urls: Seq[MediaObjectId])
  case class AddCompanyUrl(id: CompanyId, urls: Seq[MediaObjectId])
  case class AddModerationLocationUrl(id: PreModerationLocationId, urls: Seq[MediaObjectId])
  case class AddLocationUrl(id: LocationId, urls: Seq[MediaObjectId])
  case class AddCheckinLocationMedia(id: CheckinId, url: MediaObjectId)
  case class AddCheckinUserMedia(id: CheckinId, url: MediaObjectId)

}

class UrlsWriteActor extends Actor with AppActors {

  import UrlsWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val urlsDataActor = lookup(UrlsDataActor.Id)

  def receive = {

    case AddPhoto(id, url) => makeUrlsEntity(id.id, EntityType.photo, url)
    case AddPhotoAlbumFrontPhoto(id, url) => makeUrlsEntity(id.id, EntityType.photoAlbum, url)
    case AddUserMediaUrl(id, url) => makeUrlsEntity(id.id, EntityType.user, url)
    case AddUserBackgroundUrl(id, url) => makeUrlsEntity(id.id, EntityType.user, url, Some("um.bg"))
    case AddLocationPhoto(id, url) => makeUrlsEntity(id.id, EntityType.locationPhoto, url)
    case AddLocationPhotoAlbum(id, url) => makeUrlsEntity(id.id, EntityType.locationPhotoAlbum, url)
    case AddProductMedia(id, urls) => urls.foreach(url => makeUrlsEntity(id.id, EntityType.product, url))
    case AddPresentUrl(id, url) => makeUrlsEntity(id.id, EntityType.present, url)
    case AddModerationCompanyUrl(id, urls) => urls.foreach(url => makeUrlsEntity(id.id, EntityType.companyModeration, url))
    case AddCompanyUrl(id, urls) => urls.foreach(url => makeUrlsEntity(id.id, EntityType.company, url))
    case AddModerationLocationUrl(id, urls) => urls.foreach(url => makeUrlsEntity(id.id, EntityType.locationModeration, url))
    case AddLocationUrl(id, urls) => urls.foreach(url => makeUrlsEntity(id.id, EntityType.location, url))
    case AddCheckinLocationMedia(id, url) => makeUrlsEntity(id.id, EntityType.checkinLocation, url, Some("l"))
    case AddCheckinUserMedia(id, url) => makeUrlsEntity(id.id, EntityType.checkinUser, url, Some("u"))

  }

  def makeUrlsEntity(entityId: ObjectId, entityType: EntityType, mediaId: MediaObjectId, entityField: Option[String] = None) {
    urlsDataActor ! UrlsDataActor.SaveUrls(Urls(entityType = entityType, entityId = entityId, mediaId = mediaId, entityField = entityField))
  }

}
