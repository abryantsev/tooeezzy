package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import java.util.Date

@WritingConverter
class UserEventWriteConverter extends Converter[UserEvent, DBObject] with UserEventConverter {

  def convert(source: UserEvent) = UserEventConverter.serialize(source)
}

@ReadingConverter
class UserEventReadConverter extends Converter[DBObject, UserEvent] with UserEventConverter {

  def convert(source: DBObject) = UserEventConverter.deserialize(source)
}

trait UserEventConverter {

  import DBObjectConverters._

  implicit val PromotionInvitationConverter = new DBObjectConverter[PromotionInvitation] {
    def serializeObj(obj: PromotionInvitation) = DBObjectBuilder()
      .field("lid").value(obj.locationId)
      .field("pid").value(obj.promotionId)
      .field("m").value(obj.message)

    def deserializeObj(source: DBObjectExtractor) = PromotionInvitation(
      locationId  = source.field("lid").value[LocationId],
      promotionId = source.field("pid").value[PromotionId],
      message     = source.field("m").opt[String]
    )
  }

  implicit val InvitationConverter = new DBObjectConverter[Invitation] {
    def serializeObj(obj: Invitation) = DBObjectBuilder()
      .field("lid").value(obj.locationId)
      .field("m").value(obj.message)
      .field("t").value(obj.dateTime)

    def deserializeObj(source: DBObjectExtractor) = Invitation(
      locationId = source.field("lid").value[LocationId],
      message = source.field("m").opt[String],
      dateTime = source.field("t").opt[Date]
    )
  }

  implicit val FriendshipInvitationConverter = new DBObjectConverter[FriendshipInvitation] {
    def serializeObj(obj: FriendshipInvitation) = DBObjectBuilder()
      .field("fid").value(obj.id)

    def deserializeObj(source: DBObjectExtractor) =
      FriendshipInvitation(
        id = source.field("fid").value[FriendshipRequestId]
      )
  }

  implicit val UserEventPresentConverter = new DBObjectConverter[UserEventPresent] {
    def serializeObj(obj: UserEventPresent) = DBObjectBuilder()
      .field("prid").value(obj.presentId)
      .field("pid").value(obj.productId)
      .field("lid").value(obj.locationId)
      .field("m").value(obj.message)

    def deserializeObj(source: DBObjectExtractor) = UserEventPresent(
      presentId = source.field("prid").value[PresentId],
      productId = source.field("pid").value[ProductId],
      locationId = source.field("lid").value[LocationId],
      message = source.field("m").opt[String]
    )
  }

  implicit val UserEventPhotoLikeConverter = new DBObjectConverter[UserEventPhotoLike] {
    def serializeObj(obj: UserEventPhotoLike) = DBObjectBuilder()
      .field("pid").value(obj.photoId)

    def deserializeObj(source: DBObjectExtractor) = UserEventPhotoLike(
      photoId = source.field("pid").value[PhotoId]
    )
  }

  implicit val UserEventNewsLikeConverter = new DBObjectConverter[UserEventNewsLike] {
    def serializeObj(obj: UserEventNewsLike) = DBObjectBuilder()
      .field("nid").value(obj.newsId)

    def deserializeObj(source: DBObjectExtractor) = UserEventNewsLike(
      newsId = source.field("nid").value[NewsId]
    )
  }

  implicit val UserEventPhotoCommentConverter = new DBObjectConverter[UserEventPhotoComment] {
    def serializeObj(obj: UserEventPhotoComment) = DBObjectBuilder()
      .field("m").value(obj.message)
      .field("pid").value(obj.photoId)

    def deserializeObj(source: DBObjectExtractor) = UserEventPhotoComment(
      message = source.field("m").value[String],
      photoId = source.field("pid").value[PhotoId]
    )
  }

  implicit val UserEventCommentConverter = new DBObjectConverter[UserEventComment] {
    def serializeObj(obj: UserEventComment) = DBObjectBuilder()
      .field("m").value(obj.message)
      .field("nid").value(obj.newsId)
      .field("om").value(obj.originalMessage)

    def deserializeObj(source: DBObjectExtractor) = UserEventComment(
      message = source.field("m").value[String],
      newsId = source.field("nid").value[NewsId],
      originalMessage = source.field("om").opt[String]
    )
  }

  implicit val UserEventNewsConverter = new DBObjectConverter[UserEventNews] {
    def serializeObj(obj: UserEventNews) = DBObjectBuilder()
      .field("id").value(obj.newsId)
      .field("type").value(obj.newsTypeId)
      .field("header").value(obj.header)

    def deserializeObj(source: DBObjectExtractor) = UserEventNews(
      newsId = source.field("id").value[NewsId],
      newsTypeId = source.field("type").value[NewsTypeId],
      header = source.field("header").value[String]
    )
  }

  implicit val UserEventConverter = new DBObjectConverter[UserEvent] {
    def serializeObj(obj: UserEvent) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("et").value(obj.eventTypeId)
      .field("t").value(obj.createdAt)
      .field("cs").value(obj.status)
      .field("aid").value(obj.actorId)
      .field("ip").value(obj.promotionInvitation)
      .field("i").value(obj.invitation)
      .field("if").value(obj.friendshipInvitation)
      .field("p").value(obj.present)
      .field("nl").value(obj.newsLike)
      .field("pl").value(obj.photoLike)
      .field("pc").value(obj.photoComment)
      .field("c").value(obj.usersComment)

    def deserializeObj(source: DBObjectExtractor) = UserEvent(
      id = source.id.value[UserEventId],
      userId               = source.field("uid").value[UserId],
      eventTypeId          = source.field("et").value[UserEventTypeId],
      createdAt            = source.field("t").value[Date],
      status               = source.field("cs").opt[UserEventStatus],
      actorId              = source.field("aid").opt[UserId],
      promotionInvitation  = source.field("ip").opt[PromotionInvitation],
      invitation           = source.field("i").opt[Invitation],
      friendshipInvitation = source.field("if").opt[FriendshipInvitation],
      present              = source.field("p").opt[UserEventPresent],
      newsLike             = source.field("nl").opt[UserEventNewsLike],
      photoLike            = source.field("pl").opt[UserEventPhotoLike],
      photoComment         = source.field("pc").opt[UserEventPhotoComment],
      usersComment         = source.field("c").opt[UserEventComment]
    )
  }
}