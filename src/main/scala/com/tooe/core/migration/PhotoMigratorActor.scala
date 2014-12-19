package com.tooe.core.migration

import com.tooe.core.usecase._
import java.util.Date
import org.bson.types.ObjectId
import scala.concurrent.Future
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.util.Lang
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain.{Photo, PhotoAlbum}
import com.tooe.core.usecase.PhotoAlbumDataActor.{ChangePhotoCounter, CreatePhotoAlbum}
import com.tooe.core.domain.MediaObject
import com.tooe.core.usecase.PhotoWriteActor.CommentPhoto
import com.tooe.core.usecase.PhotoCreated
import scala.Some
import com.tooe.core.domain.UserId
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.domain.PhotoAlbumId
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.api.service.DigitalSign
import com.tooe.core.migration.db.domain.IdMapping
import com.tooe.core.migration.DictionaryIdMappingActor.GetUserGroup
import com.tooe.core.domain.PhotoId
import com.tooe.core.usecase.PhotoWriteActor.LikePhoto
import com.tooe.api.service.PhotoMessage
import com.tooe.core.usecase.user.UserDataActor.AddPhotoToUser

object PhotoMigratorActor {

  val Id = 'photoMigratorActor

  case class LegacyPhotoComment(msg: String, userid: Int, time: Date)
  case class LegacyPhotoLike(userid: Int, time: Date)
  case class LegacyPhotoUserGroups(view: Option[Seq[Int]], comments: Option[Seq[Int]])
  case class LegacyPhoto(legacyid: Int, name: Option[String], time: Date, url: String,
                         likes: Seq[LegacyPhotoLike], comments: Option[Seq[LegacyPhotoComment]])
  case class LegacyPhotoAlbum(legacyid: Int, time: Date, userid: Int, name: String,
                              description: String, usergroups: Option[LegacyPhotoUserGroups],
                              mainphotoid: Option[Int], photos: Seq[LegacyPhoto]) extends UnmarshallerEntity
}

class PhotoMigratorActor extends MigrationActor {
  import com.tooe.core.migration.PhotoMigratorActor._

  def receive = {
    case la: LegacyPhotoAlbum => {
      import la._
      val future =
        for {
          userId <- lookupByLegacyId(la.userid, MappingCollection.user).map(UserId)
          (view, comment) <- groupify(la)
          paId <- createPhotoAlbum(la, userId, view, comment)
          _ <- photosWrite(photos, paId, userId)
          _ <- idMappingDataActor ? IdMappingDataActor.SaveIdMapping(IdMapping(new ObjectId(), MappingCollection.userPhotoAlbum, legacyid, paId.id))
        } yield {
          MigrationResponse(DefaultMigrationResult(la.legacyid, paId.id, "photo_migrator"))
        }
      future pipeTo sender
    }
  }

  def createPhotoAlbum(lpa: LegacyPhotoAlbum, uid: UserId, view: Seq[String], comment: Seq[String]): Future[PhotoAlbumId] = {
    import lpa._
    val paid = PhotoAlbumId(new ObjectId)
    val photoAlbum = PhotoAlbum(id = paid, name = name, description = Some(description), userId = UserId(uid.id),
      count = 0, frontPhotoUrl = MediaObject(MediaObjectId(photos.head.url), UrlType.MigrationType), allowedView = view, allowedComment = comment, createdTime = time)

    photos.headOption.foreach {
      ph => saveUrl(EntityType.photoAlbum, paid.id, ph.url, UrlField.UserPhotoAlbumMain)
    }

    userDataActor ! UserDataActor.AddPhotoAlbum(uid, paid)
    updateStatisticActor ! UpdateStatisticActor.ChangeUsersPhotoAlbumsCounter(uid, 1)
    (photoAlbumDataActor ? CreatePhotoAlbum(photoAlbum)).mapTo[PhotoAlbumId]
  }

  def photosWrite(lps: Seq[LegacyPhoto], albumId: PhotoAlbumId, userId: UserId): Future[Seq[PhotoCreated]] = {
    def photoWrite(lp: LegacyPhoto, albumId: PhotoAlbumId, userId: UserId): Future[PhotoCreated] = {
      val photo = Photo(userId = userId, name = lp.name, fileUrl = MediaObject(MediaObjectId(lp.url), UrlType.MigrationType), photoAlbumId = albumId, createdAt = lp.time)
      val result = for {
        savedPhoto <- (photoDataActor ? PhotoDataActor.CreatePhoto(photo)).mapTo[Photo]
      } yield {
        userDataActor ! AddPhotoToUser(userId, photo.id)
        saveUrl(EntityType.photo, photo.id.id, lp.url, UrlField.UserPhoto)
        photoAlbumDataActor ! ChangePhotoCounter(albumId, 1)
        PhotoCreated(PhotoCreatedId(savedPhoto.id))
      }
      result.mapTo[PhotoCreated].flatMap {
        photoCreated =>
          (idMappingDataActor ? IdMappingDataActor.SaveIdMapping(IdMapping(new ObjectId(), MappingCollection.userPhoto, lp.legacyid, photoCreated.photo.id.id))).map(_ => photoCreated)
      }
    }

    def photoLikesWrite(likes: Seq[LegacyPhotoLike], photoId: PhotoId): Future[Int] = {
      Future.sequence(likes map {
        ll =>
          lookupByLegacyId(ll.userid, MappingCollection.user) flatMap {
            userId =>
              photoWriteActor ? LikePhoto(photoId, UserId(userId))
          }
      }).map(_.length)
    }

    def photoCommentsWrite(comments: Option[Seq[LegacyPhotoComment]], photoId: PhotoId): Future[Int] = {
      comments match {
        case None => Future successful 0
        case Some(commentSeq) => Future.sequence(commentSeq.map {
          pc =>
            lookupByLegacyId(pc.userid, MappingCollection.user) flatMap {
              userId =>
                photoWriteActor ? CommentPhoto(photoId, UserId(userId), PhotoMessage(pc.msg), DigitalSign(None), Lang.ru)
            }
        }).map(_.length)
      }
    }
    Future.traverse[LegacyPhoto, PhotoCreated, Seq](lps.sortBy(lp => lp.time.getTime)) {
      lp =>
        for {
          photoCreated <- photoWrite(lp, albumId, userId)
          likes <- photoLikesWrite(lp.likes, photoCreated.photo.id)
          comments <- photoCommentsWrite(lp.comments, photoCreated.photo.id)
        } yield photoCreated
    }
  }

  def groupify(la: LegacyPhotoAlbum): Future[(Seq[String], Seq[String])] = {
    la.usergroups map {
      case lpug: LegacyPhotoUserGroups =>
        val viewers = lpug.view.getOrElse(Nil)
        Future.sequence((viewers ++ lpug.comments.getOrElse(Nil)).map {
          id =>
            (dictionaryIdMapping ? GetUserGroup(id)).mapTo[String]
        }).map {
          xs =>
            val (view, comment) = xs.splitAt(viewers.length)
            (view.filter(!_.isEmpty), comment.filter(!_.isEmpty))
        }
    } getOrElse (Future successful(Nil, Nil))
  }

  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val photoAlbumDataActor = lookup(PhotoAlbumDataActor.Id)
  lazy val photoWriteActor = lookup(PhotoWriteActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val dictionaryIdMapping = lookup(DictionaryIdMappingActor.Id)
}
