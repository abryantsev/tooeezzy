package com.tooe.core.usecase

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe}
import com.tooe.core.service.{MediaObjectFixture, LocationPhotoFixture, LocationFixture}
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumWriteActor
import com.tooe.api.service.{AddLocationPhotoAlbumRequest, RouteContext, AddPhotoParams}
import com.tooe.core.domain.{LocationMainPhoto, LocationPhotoAlbumId, LocationId}
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.{LocationPhoto, LocationPhotoAlbum}

class LocationPhotoAlbumWriteActorTest extends ActorTestSpecification {

  "LocationPhotoAlbumWriteActor component methods" should {
    "increment photoAlbum counter" >> {
      val f = new LocationPhotoAlbumWriteActorFixture {}
      import f._
      locationPhotoAlbumWriteActor ! LocationPhotoAlbumWriteActor.CreateLocationPhotoAlbum(request, null)
      updateStatisticProbe.expectMsgAllOf(
        UpdateStatisticActor.ChangeLocationPhotoAlbumsCounter(location.id, 1)
      )
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class LocationPhotoAlbumWriteActorFixture(implicit system: ActorSystem) {
  val savedPhotoFilePath = "filePath"
  val albumName = "albumName"
  val locationPhotoAlbumId = LocationPhotoAlbumId()
  val location = new LocationFixture().entity
  val locationPhoto = new LocationPhotoFixture().locationPhoto
  val request = AddLocationPhotoAlbumRequest(location.id, albumName, None, LocationMainPhoto(new MediaObjectFixture().mediaObject.url, None))

  val updateStatisticProbe = TestProbe()

  class LocationPhotoAlbumWriteActorUnderTest extends LocationPhotoAlbumWriteActor {

    override lazy val updateStatisticActor: ActorRef = updateStatisticProbe.ref

    override def addLocationPhotoAlbum(album: LocationPhotoAlbum): Future[LocationPhotoAlbumId] = Future.successful(locationPhotoAlbumId)

    override def saveLocationPhoto(photo: LocationMainPhoto, locationId: LocationId, albumId: LocationPhotoAlbumId) =
      Future successful(locationPhoto)

    override def getLocation(id: LocationId) = Future successful location
    override def getActiveOrDeactivatedLocation(id: LocationId) = Future successful location
  }

  def locationPhotoAlbumActorFactory: LocationPhotoAlbumWriteActor = new LocationPhotoAlbumWriteActorUnderTest

  lazy val locationPhotoAlbumWriteActor = TestActorRef[LocationPhotoAlbumWriteActor](Props(locationPhotoAlbumActorFactory))
}
