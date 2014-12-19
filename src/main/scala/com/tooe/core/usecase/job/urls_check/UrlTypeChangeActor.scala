package com.tooe.core.usecase.job.urls_check

import com.tooe.core.application.Actors
import com.tooe.core.usecase.{PhotoAlbumDataActor, PhotoDataActor, AppActor}
import com.tooe.core.domain.EntityType
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.usecase.location.{PreModerationLocationDataActor, LocationDataActor}
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumDataActor
import com.tooe.core.usecase.locationschain.LocationsChainDataActor
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.usecase.present.PresentDataActor
import com.tooe.core.usecase.company.{PreModerationCompanyDataActor, CompanyDataActor}
import akka.actor.ActorRef
import akka.io.IO
import spray.can.Http
import spray.httpx.RequestBuilding._
import com.tooe.core.db.mongo.domain.Urls
import com.tooe.core.usecase.checkin.CheckinDataActor

object UrlTypeChangeActor {
  val Id = Actors.urlTypeChange

}

class UrlTypeChangeActor extends AppActor {

  import ChangeUrlType._

  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val photoAlbumDataActor = lookup(PhotoAlbumDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val preModerationLocationDataActor = lookup(PreModerationLocationDataActor.Id)
  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)
  lazy val locationPhotoAlbumDataActor = lookup(LocationPhotoAlbumDataActor.Id)
  lazy val locationChainDataActor = lookup(LocationsChainDataActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val presentDataActor = lookup(PresentDataActor.Id)
  lazy val companyDataActor = lookup(CompanyDataActor.Id)
  lazy val preModerationCompanyDataActor = lookup(PreModerationCompanyDataActor.Id)
  lazy val checkinDataActor = lookup(CheckinDataActor.Id)

  lazy val httpClient = IO(Http)(context.system)
  lazy val mediaServerUrl = settings.MediaServer.Host

  def receive = {
    case m@ChangeTypeToS3(url, _) =>
      actorByType(url) ! m
    case m@ChangeTypeToCDN(url) =>
      actorByType(url) ! m
      httpClient ! Post(revokeS3Url(url))
  }

  def revokeS3Url(url: Urls) =
    s"${mediaServerUrl}revokeS3rights/${getMediaServerSuffixByEntityType(url)}?mediaobjectID=${url.mediaId.id}"


  def actorByType(url: Urls): ActorRef = url.entityType match {
    case EntityType.user => userDataActor
    case EntityType.photoAlbum => photoAlbumDataActor
    case EntityType.photo => photoDataActor
    case EntityType.location => locationDataActor
    case EntityType.locationPhotoAlbum => locationPhotoAlbumDataActor
    case EntityType.locationPhoto => locationPhotoDataActor
    case EntityType.locationsChain => locationChainDataActor
    case EntityType.product => productDataActor
    case EntityType.present => presentDataActor
    case EntityType.companyModeration => preModerationCompanyDataActor
    case EntityType.company => companyDataActor
    case EntityType.locationModeration => preModerationLocationDataActor
    case u if u == EntityType.checkinLocation || u == EntityType.checkinUser => checkinDataActor
  }

}
