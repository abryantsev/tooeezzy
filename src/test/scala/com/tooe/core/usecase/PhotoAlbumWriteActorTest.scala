package com.tooe.core.usecase

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import com.tooe.core.db.mongo.domain.{PhotoAlbum, UserFixture}
import com.tooe.api.service.AddPhotoParams
import concurrent.Future
import com.tooe.core.domain.PhotoAlbumId
import com.tooe.core.service.{PhotoFixture, PhotoAlbumFixture}
import com.tooe.core.usecase.user.UserDataActor

class PhotoAlbumWriteActorTest extends ActorTestSpecification {

  "PhotoAlbumWriteActor" should {

    "send ChangeUsersPhotoAlbumsCounter on creating album " >> {
      val f = new PhotoAlbumWriteActorBaseFixture() {
        val photoAlbum = new PhotoAlbumFixture().photoAlbum.copy(userId = user.id)
        val addPhotoParams = AddPhotoParams(photoAlbum.frontPhotoUrl.url.id, None, name = Some("photo-name"))

        def testActorFactory = new PhotoAlbumWriteActorUnderTest {
          override def createPhotoAlbum(request: PhotoAlbumWriteActor.AddPhotoAlbum) =
            Future successful (photoAlbum.id, null)
        }
      }
      import f._

      photoAlbumWriteActor ! PhotoAlbumWriteActor.AddPhotoAlbum(user.id, None, None, None, addPhotoParams, null)

      userDataProbe expectMsg UserDataActor.AddPhotoAlbum(user.id, photoAlbum.id)
      updateStatisticProbe expectMsg UpdateStatisticActor.ChangeUsersPhotoAlbumsCounter(user.id, 1)
      success
    }
    "send ChangeUsersPhotoAlbumsCounter on deleting album " >> {
      val f = new PhotoAlbumWriteActorBaseFixture {
        val photo = new PhotoFixture().photo
        val photoAlbumId = PhotoAlbumId()

        def testActorFactory = new PhotoAlbumWriteActorUnderTest {
          override def getAllPhotosByAlbum(albumId: PhotoAlbumId) = Future successful List(photo)
        }
      }
      import f._

      photoAlbumWriteActor ! PhotoAlbumWriteActor.DeletePhotoAlbum(photoAlbumId, user.id)

      userDataProbe expectMsg UserDataActor.RemovePhotoAlbum(user.id, photoAlbumId)
      updateStatisticProbe expectMsg UpdateStatisticActor.ChangeUsersPhotoAlbumsCounter(user.id, -1)
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class PhotoAlbumWriteActorBaseFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {
  val user = new UserFixture().user
  val updateStatisticProbe, userDataProbe = TestProbe()

  class PhotoAlbumWriteActorUnderTest extends PhotoAlbumWriteActor {
    override lazy val updateStatisticActor = updateStatisticProbe.ref
    override lazy val userDataActor = userDataProbe.ref

    override def getAlbumFtr(albumId: PhotoAlbumId): Future[PhotoAlbum] = Future successful new PhotoAlbumFixture().photoAlbum

  }

  def testActorFactory: PhotoAlbumWriteActor

  lazy val photoAlbumWriteActor = TestActorRef[PhotoAlbumWriteActor](Props(testActorFactory))
}