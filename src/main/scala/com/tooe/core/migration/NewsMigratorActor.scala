package com.tooe.core.migration

import java.util.Date
import scala.concurrent.Future
import scala.Some
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.promotion.PromotionDataActor
import com.tooe.core.usecase.present.PresentDataActor
import com.tooe.core.usecase.wish.WishDataActor
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.db.mongo.domain._
import com.tooe.core.usecase._
import com.tooe.core.domain._
import com.tooe.core.migration.db.domain.MappingCollection

object NewsMigratorActor {
  val Id = 'newsMigratorActor

  private[migration] object LegacyStuffHolder {
    case class LLike(userid: Int, time: Date)
    case class LComment(id: Int, parentid: Option[Int], msg: String, authorid: Int, time: Date)
    case class LCheckin(locationid: Int)
    case class LFavorite(locationid: Int)
    case class LUserComment(msg: String)
    case class LSubscription(locationid: Int)
    case class LWish(wishid: Int)
    case class LPresent(presentid: Int)
    case class LPhotoAlbum(photoalbumid: Int)
    case class LPhoto(photoalbumid: Int, photoids: Seq[Int])
  }

  import LegacyStuffHolder._
  case class LegacyNews(legacyid: Int, userids: Seq[Int], eventtypeid: Int, time: Date, actorid: Option[Int], recipientid: Option[Int], likes: Seq[LLike], comments: Seq[LComment], checkin: Option[LCheckin], favorite: Option[LFavorite],
                        subscription: Option[LSubscription], usercomment: Option[LUserComment], wish: Option[LWish], present: Option[LPresent], photoalbum: Option[LPhotoAlbum], photo: Option[LPhoto]) extends UnmarshallerEntity
}

class NewsMigratorActor extends MigrationActor {
  import NewsMigratorActor._

  def receive = {
    case ln: LegacyNews =>
      (for {
        news <- upgradeNews(ln)
        _ <- saveNews(news)
      } yield MigrationResponse(DefaultMigrationResult(ln.legacyid, news.id.id, "news_migrator"))).pipeTo(sender)
  }

  def upgradeNews(ln: LegacyNews): Future[News] = {
    import mappers._
    implicit val legacy = ln
    val nid = NewsId()
    for {
      users <- mapUsers
      actor <- mapActorId
      newsType <- mapNewsTypeIdAndFilter
      recipient <- mapRecipient
      (likers, newLikes) <- mapLikes(nid)
      (shorts, newComments) <- mapComments(nid)
      /*checkIn <- mapCheckIn*/
      favorite <- mapFavoriteLocation
      /*subscription <- mapSubscription*/
      userComment <- mapUserComment
      wish <- mapWish
      present <- mapPresent
      album <- mapPhotoAlbum
      _ = saveComments(newComments)
      _ = saveLikes(newLikes)
    } yield News(id = nid,
      viewers = users,
      newsType = newsType,
      createdAt = ln.time,
      actorId = actor,
      recipientId = recipient,
      actorsAndRecipientsIds = actor.toSeq ++ recipient.toSeq,
      likesCount = ln.likes.length,
      usersWhoLike = likers,
      commentsCount = ln.comments.length,
      comments = shorts,
      /*checkin = checkIn,*/
      favoriteLocation = favorite,
      /*subscription = subscription,*/
      userComment = userComment,
      wish = wish,
      present = present,
      photoAlbum = album)
  }

  def saveNews(event: News): Future[News] = {
    (newsDataActor ? NewsDataActor.Save(event)).mapTo[News]
  }

  def saveLikes(ls: Seq[NewsLike]) =
    ls.foreach(like => newsLikeDataActor ! NewsLikeDataActor.Save(like))

  def saveComments(com: Seq[NewsComment]) =
    com.foreach(comment => newsCommentDataActor ! NewsCommentDataActor.Save(comment))

  object mappers {
    import com.tooe.core.migration.db.domain.MappingCollection._
    def mapRecipient(implicit ln: LegacyNews): Future[Option[UserId]] = ln.recipientid.map(id =>
      lookupByLegacyId(id, user).map(UserId))
    def mapUsers(implicit ln: LegacyNews): Future[Seq[UserId]] = {
      getIdMappings(ln.userids, user).mapInner(UserId)
    }
    def mapNewsTypeIdAndFilter(implicit ln: LegacyNews) = (dictIdMappingActor ? DictionaryIdMappingActor.GetEventType(ln.eventtypeid)).mapTo[String].map {
      id =>
        if (migratedNews.contains(id))
          NewsTypeId(id)
        else
          throw new MigrationException(s"news of type $id are not migrated")
    }
    def mapActorId(implicit ln: LegacyNews): Future[Option[UserId]] = ln.actorid.map(lid =>
      lookupByLegacyId(lid, user).map(UserId))
    def mapLikes(newsId: NewsId)(implicit ln: LegacyNews): Future[(Seq[UserId], Seq[NewsLike])] = {
      val uidsAndLLikes = getIdMappings(ln.likes.map(_.userid), user).map(_ zip ln.likes)
      uidsAndLLikes.mapInner {
        case (uid, ll) =>
          (UserId(uid),
            NewsLike(NewsLikeId(new ObjectId), newsId, ll.time, UserId(uid)))
      }.map(_.unzip)
    }
    def mapComments(nid: NewsId)(implicit ln: LegacyNews): Future[(Seq[NewsCommentShort], Seq[NewsComment])] = {
      val (users, parents) = ln.comments.map(lc => (lc.authorid, lc.parentid)).unzip
      val cidMap = parents.distinct.map(par => (par, par.map(_ => NewsCommentId(new ObjectId())))).toMap.withDefault(_ => Some(NewsCommentId(new ObjectId)))
      getIdMappings(users, user).mapInner(UserId).map(ids => users.zip(ids).toMap).map {
        case uidMap => ln.comments.map {
          lc =>
            val id = cidMap(Some(lc.id)).get
            val pid = cidMap(lc.parentid)
            val uid = uidMap(lc.authorid)
            (NewsCommentShort(id, pid, lc.time, lc.msg, uid),
              NewsComment(id, pid, nid, lc.time, lc.msg, uid))
        }
      }.map(_.unzip)
    }
    /*def mapCheckIn(implicit ln: LegacyNews): Future[Option[NewsCheckin]] = ln.checkin.map(ci => lookupByLegacyId(ci.locationid, location).map(lid => NewsCheckin(LocationId(lid))))*/
    def mapFavoriteLocation(implicit ln: LegacyNews): Future[Option[NewsFavoriteLocation]] = ln.favorite.map(lf => lookupByLegacyId(lf.locationid, location).map(lid => NewsFavoriteLocation(LocationId(lid))))
    /*def mapSubscription(implicit ln: LegacyNews): Future[Option[NewsSubscription]] = ln.subscription.map(ls => lookupByLegacyId(ls.locationid, location).map(lid => NewsSubscription(LocationId(lid))))*/
    def mapUserComment(implicit ln: LegacyNews): Future[Option[NewsUserComment]] = Future successful ln.usercomment.map(luc => NewsUserComment(luc.msg))
    def mapWish(implicit ln: LegacyNews): Future[Option[NewsWish]] = ln.wish.map(lw => lookupByLegacyId(lw.wishid, wish).map(WishId).flatMap(weed =>
      (wishDataActor ? WishDataActor.GetWish(weed)).mapTo[Wish].map(wish => NewsWish(weed, wish.product.locationId, wish.product.productId))))
    def mapPresent(implicit ln: LegacyNews): Future[Option[NewsPresent]] = ln.present.map(lp => lookupByLegacyId(lp.presentid, present).map(PresentId).flatMap(pid =>
      (presentDataActor ? PresentDataActor.GetPresent(pid)).mapTo[Present].map(pr => NewsPresent(pr.product.locationId, pr.product.productId, pr.message))))
    def mapPhotoAlbum(implicit ln: LegacyNews): Future[Option[NewsPhotoAlbum]] = ln.photoalbum.map(lp => lookupByLegacyId(lp.photoalbumid, userPhotoAlbum).flatMap(
      id =>
        (photoAlbumDataActor ? PhotoAlbumDataActor.GetPhotoAlbumById(PhotoAlbumId(id))).mapTo[PhotoAlbum].flatMap(pa =>
          (photoDataActor ? PhotoDataActor.GetAllPhotosByAlbum(pa.id)).mapTo[List[Photo]].map(_.sortBy(-_.likesCount).take(10)).map(photos =>
            NewsPhotoAlbum(pa.id, 0, photos.map(_.id)))))).orElse(ln.photo.map {   // TODO set photosCount properly
      photo =>
        for {
          albumId <- lookupByLegacyId(photo.photoalbumid, MappingCollection.userPhotoAlbum)
          photoIds <- getIdMappings(photo.photoids, MappingCollection.userPhoto)
          album <- (photoAlbumDataActor ? PhotoAlbumDataActor.GetPhotoAlbumById(PhotoAlbumId(albumId))).mapTo[PhotoAlbum]
          photos <- (photoDataActor ? PhotoDataActor.GetPhotos(photoIds.map(PhotoId))).mapTo[Seq[Photo]]
        } yield NewsPhotoAlbum(album.id, 0, photos.map(_.id)) // TODO set photosCount properly
    })
  }

  val migratedNews = Set("friend", "message",
    "present", "photo", "photoalbum",
    "wish", "favoritelocation",
    "eventvisitor", "userscomment")

  lazy val newsLikeDataActor = lookup(NewsLikeDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val photoAlbumDataActor = lookup(PhotoAlbumDataActor.Id)
  lazy val wishDataActor = lookup(WishDataActor.Id)
  lazy val newsCommentDataActor = lookup(NewsCommentDataActor.Id)
  lazy val newsDataActor = lookup(NewsDataActor.Id)
  lazy val promotionDataActor = lookup(PromotionDataActor.Id)
  lazy val dictIdMappingActor = lookup(DictionaryIdMappingActor.Id)
  lazy val presentDataActor = lookup(PresentDataActor.Id)
}
