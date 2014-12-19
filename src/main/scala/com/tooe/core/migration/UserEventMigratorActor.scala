package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import java.util.Date
import com.tooe.core.usecase.AppActor
import com.tooe.core.migration.db.domain.MappingCollection._
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import com.tooe.core.usecase.promotion.PromotionDataActor
import com.tooe.core.domain.PromotionId
import com.tooe.core.migration.DictionaryIdMappingActor.GetEventType
import com.tooe.core.domain.UserEventId
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.PromotionInvitation
import com.tooe.core.db.mongo.domain.UserEvent
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.usecase.present.PresentDataActor
import com.tooe.core.migration.api.{DefaultMigrationResult, MigrationResponse}
import com.tooe.core.usecase.user_event.UserEventDataActor
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.friendshiprequest.FriendshipRequestDataActor

object UserEventMigratorActor {
  val Id = 'userEventMigrator
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit def optOfFut2futOfOpt[A](value: Option[Future[A]]): Future[Option[A]] = Future.sequence(value.toTraversable).map(_.headOption)

  private[migration] object LegacyStuffHolder {
    /*case class LPromoInvite(message: String, promotionid: Int)
    case class LInvite(message: String, locationid: Int, meetdate: Option[Date])*/
    case class LFriendRequest(friendshiprequestid: Int)
    case class LPresent(message: Option[String], presentid: Int)
    /*case class LNewsLike(newsid: Int)*/
    /*case class LPhotoLike(photoid: Int)*/
    /*case class LPhotoComment(message: String, photoid: Int)*/
  }

  import LegacyStuffHolder._
  case class LUserEvent
  (legacyid: Int,
   userid: Int,
   eventtypeid: Int,
   time: Date,
   status: Option[String],
   actorid: Option[Int],
   invitetofriends: Option[LFriendRequest],
   present: Option[LPresent]) extends UnmarshallerEntity
}

class UserEventMigratorActor extends MigrationActor {
  import UserEventMigratorActor._
  import mappers._

  def receive = {
    case lue: LUserEvent =>
      implicit val implicitLue = lue
      val eventFuture = for {
        uid <- lookupByLegacyId(lue.userid, user).map(UserId)
        eventType <- mapEventTypeId
        actor <- mapActorId
        present <- mapPresent
        friendReq <- mapFriendRequest
        event = UserEvent(UserEventId(new ObjectId()), uid, eventType, lue.time, mapStatus, actor, present = present, friendshipInvitation = friendReq)
        same <- saveEvent(event)
      } yield MigrationResponse(DefaultMigrationResult(lue.legacyid, event.id.id, "userevent_migrator"))
      eventFuture pipeTo sender
  }

  def saveEvent(event: UserEvent): Future[Any] = {
    userEventDataActor ? UserEventDataActor.Save(event)
  }

  object mappers {
    def mapEventTypeId(implicit lue: LUserEvent) =
      (dictIdMappingActor ? GetEventType(lue.eventtypeid)).mapTo[String].map(id => UserEventTypeId(id))
    def mapStatus(implicit lue: LUserEvent): Option[UserEventStatus] = lue.status.map {
      case "confirmed" => UserEventStatus.Confirmed
      case "rejected" => UserEventStatus.Rejected
    }
    def mapActorId(implicit lue: LUserEvent): Future[Option[UserId]] = lue.actorid.map(lid =>
      lookupByLegacyId(lid, user).map(UserId)
    )
    def mapFriendRequest(implicit lue: LUserEvent): Future[Option[FriendshipInvitation]] = lue.invitetofriends.map(lfr =>
      lookupByLegacyId(lfr.friendshiprequestid, MappingCollection.friendshipRequest).map(
        id =>
          FriendshipInvitation(FriendshipRequestId(id))))
    def mapPresent(implicit lue: LUserEvent): Future[Option[UserEventPresent]] = lue.present.map(pr =>
      lookupByLegacyId(pr.presentid, present).flatMap(pid =>
        (presentDataActor ? PresentDataActor.FindPresent(PresentId(pid))).mapTo[Option[Present]].map {
          opt =>
            val present = opt.getOrElse(throw new MigrationException(s"present $pid not found for event"))
            import present._
            UserEventPresent(id, product.productId, product.locationId, pr.message)
        }))
  }

  lazy val userEventDataActor = lookup(UserEventDataActor.Id)
  lazy val dictIdMappingActor = lookup(DictionaryIdMappingActor.Id)
  lazy val presentDataActor = lookup(PresentDataActor.Id)

}



/*def mapPhotoLike(implicit lue: LUserEvent): Future[Option[UserEventPhotoLike]] = lue.photolike.map(lLike =>
      lookupByLegacyId(lLike.photoid, userPhoto).map(pid =>
        UserEventPhotoLike(PhotoId(pid))))
    def mapPhotoComment(implicit lue: LUserEvent): Future[Option[UserEventPhotoComment]] = lue.photocomment.map(lComment =>
      lookupByLegacyId(lComment.photoid, userPhoto).map(pid =>
        UserEventPhotoComment(lComment.message, PhotoId(pid))))*/

/*def mapInviteToPromo(implicit lue: LUserEvent): Future[Option[PromotionInvitation]] = lue.invitetopromo map (i =>
      for {
        pid <- lookupByLegacyId(i.promotionid, MappingCollection.promotion)
        promo <- (promotionDataActor ? PromotionDataActor.GetPromotion(PromotionId())).mapTo[Promotion]
      } yield PromotionInvitation(promo.location.location, PromotionId(pid), Some(i.message))
      )*/
/*def mapInvite(implicit lue: LUserEvent): Future[Option[Invitation]] = lue.invite.map(inv =>
  lookupByLegacyId(inv.locationid, location).map(lid =>
    Invitation(LocationId(lid), Some(inv.message), inv.meetdate)))*/