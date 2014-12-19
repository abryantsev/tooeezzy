package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.{DateHelper, SomeWrapper, HashHelper}
import com.tooe.core.domain._
import com.tooe.core.db.mongo.query.{UpdateResult, SkipLimitSort}
import org.bson.types.ObjectId

class UserEventDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: UserEventDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("userevent")

  @Test
  def saveAndRead {
    val f = new UserEventFixture
    import f._
    service.findOne(userEvent.id) === None
    service.save(userEvent) === userEvent
    service.findOne(userEvent.id) === Some(userEvent)
  }

  @Test
  def saveMany {
    val f = new UserEventFixture
    import f._
    service.findOne(userEvent.id) === None
    service.saveMany(Seq(userEvent)) === Seq(userEvent)
    service.findOne(userEvent.id) === Some(userEvent)
  }

  @Test
  def representation {
    val f = new UserEventFixture
    import f._
    service.save(userEvent)
    val present = userEvent.present.get
    val promotion = userEvent.promotionInvitation.get
    val invitation = userEvent.invitation.get
    val repr = entities.findOne(userEvent.id.id)
    println("repr="+repr)
    jsonAssert(repr)(s"""{
      "_id" : ${userEvent.id.id.mongoRepr} ,
      "uid" : ${userEvent.userId.id.mongoRepr} ,
      "et" : ${userEvent.eventTypeId.id} ,
      "t" : ${userEvent.createdAt.mongoRepr} ,
      "cs" : ${userEvent.status.get.id} ,
      "aid" : ${userEvent.actorId.get.id.mongoRepr} ,
      "ip" : {
        "lid" : ${promotion.locationId.id.mongoRepr} ,
        "pid" : ${promotion.promotionId.id.mongoRepr} ,
        "m" : ${promotion.message.get}
      } ,
      "i" : {
        "lid" : ${invitation.locationId.id.mongoRepr} ,
        "m" : "${invitation.message.get}" ,
        "t" : ${invitation.dateTime.get.mongoRepr}
      } ,
      "if" : {
        "fid" : ${friendshipInvitation.id.id.mongoRepr}
      } ,
      "p" : {
        "prid" : ${present.presentId.id.mongoRepr} ,
        "pid" : ${present.productId.id.mongoRepr} ,
        "lid" : ${present.locationId.id.mongoRepr} ,
        "m" : ${present.message.get}
      },
      "pl" : {
        "pid" : ${userEvent.photoLike.get.photoId.id.mongoRepr}
      }
    }""")
  }

  @Test
  def findByUserId {
    val f = new UserEventFixture
    import f._
    service.find(userEvent.userId, SkipLimitSort(0, 1)) === Nil
    service.save(userEvent)
    service.find(userEvent.userId, SkipLimitSort(0, 1)) === Seq(userEvent)
    service.find(userEvent.userId, SkipLimitSort(1, 1)) === Nil
  }

  @Test
  def updateStatus {
    val f = new UserEventFixture(status = None)
    import f._
    import UserEventStatus._
    userEvent.status === None
    service.updateStatus(userEvent.id, Confirmed) === None
    service.save(userEvent)
    val updated = service.updateStatus(userEvent.id, Confirmed).get
    val found = service.findOne(userEvent.id).get
    updated === found
    updated.status.get === Confirmed
  }

  @Test
  def delete {
    val f = new UserEventFixture
    import f._
    service.save(userEvent)
    service.delete(userEvent.id)
    service.findOne(userEvent.id) === None
  }

  @Test
  def deleteByUser {
    val userId = UserId()
    val f1, f2 = new UserEventFixture(userId = userId)
    val f3 = new UserEventFixture()
    service.saveMany(Seq(f1.userEvent, f2.userEvent, f3.userEvent))
    service.delete(userId)

    service.findOne(f1.userEvent.id) === None
    service.findOne(f2.userEvent.id) === None
    service.findOne(f3.userEvent.id) !== None
  }

  @Test
  def unsetUserEventId {
    val userEvent = new UserEventFixture().userEvent
    userEvent.friendshipInvitation !== None

    service.save(userEvent)
    service.unsetFriendshipRequestId(userEvent.id) === UpdateResult.Updated

    service.findOne(userEvent.id).get.friendshipInvitation === None
  }

  @Test
  def findByFriendshipRequestId {
    val userEvent = new UserEventFixture().userEvent
    service.save(userEvent)

    service.find(userEvent.friendshipInvitation.get.id) === Some(userEvent)
  }
}

class UserEventFixture
(
  locationId: LocationId = LocationId(),
  promotionId: PromotionId = PromotionId(),
  actorId: UserId = UserId(),
  presentId: PresentId = PresentId(),
  status: Option[UserEventStatus] = Some(UserEventStatus.Confirmed),
  userId: UserId = UserId(),
  photo: Photo = new PhotoFixture().photo
  )
{
  import SomeWrapper._

  val userEventPhotoLike = UserEventPhotoLike(
    photoId = photo.id
  )

  val promotionInvitation = PromotionInvitation(
    locationId = locationId,
    promotionId = promotionId,
    message = "some-promotion-message"
  )

  val invitation = Invitation(
    locationId = locationId,
    message = "some-invitation-message",
    dateTime = DateHelper.currentDate
  )

  val friendshipInvitation = FriendshipInvitation(
    id = FriendshipRequestId()
  )

  val present = UserEventPresent(
    presentId = presentId,
    productId = ProductId(new ObjectId),
    locationId = LocationId(),
    message = "some-present-message"
  )

  val userEvent = UserEvent(
    id = UserEventId(),
    userId = userId,
    eventTypeId = UserEventTypeId(HashHelper.uuid),
    createdAt = DateHelper.currentDate,
    status = status,
    actorId = actorId,
    promotionInvitation = promotionInvitation,
    invitation = invitation,
    friendshipInvitation = friendshipInvitation,
    present = present,
    photoLike = userEventPhotoLike
  )
}