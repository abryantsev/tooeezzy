package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.{WritingConverter, ReadingConverter}
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import java.util.Date

@WritingConverter
class NewsWriteConverter extends Converter[News, DBObject] with NewsConverter {
  def convert(obj: News) = NewsConverter.serialize(obj)
}

@ReadingConverter
class NewsReadConverter extends Converter[DBObject, News] with NewsConverter {
  def convert(source: DBObject) = NewsConverter.deserialize(source)
}

trait NewsConverter {
  import DBObjectConverters._

  implicit val NewsCommentConverter = new DBObjectConverter[NewsCommentShort] {
    def serializeObj(obj: NewsCommentShort) = DBObjectBuilder()
      .id.value(obj.id)
      .field("pid").value(obj.parentId)
      .field("t").value(obj.createdAt)
      .field("m").value(obj.message)
      .field("aid").value(obj.authorId)

    def deserializeObj(source: DBObjectExtractor) = NewsCommentShort(
      id = source.id.value[NewsCommentId],
      parentId = source.field("pid").opt[NewsCommentId],
      createdAt = source.field("t").value[Date],
      message = source.field("m").value[String],
      authorId = source.field("aid").value[UserId]
    )
  }
  implicit val NewsCheckinConverter = new DBObjectConverter[NewsCheckin] {
    def serializeObj(obj: NewsCheckin) = DBObjectBuilder()
      .field("lid").value(obj.locationId)

    def deserializeObj(source: DBObjectExtractor) = NewsCheckin(
      locationId = source.field("lid").value[LocationId]
    )
  }
  implicit val NewsFavoriteLocationConverter = new DBObjectConverter[NewsFavoriteLocation] {
    def serializeObj(obj: NewsFavoriteLocation) = DBObjectBuilder()
      .field("lid").value(obj.locationId)

    def deserializeObj(source: DBObjectExtractor) = NewsFavoriteLocation(
      locationId = source.field("lid").value[LocationId]
    )
  }
  implicit val NewsSubscriptionConverter = new DBObjectConverter[NewsSubscription] {
    def serializeObj(obj: NewsSubscription) = DBObjectBuilder()
      .field("lid").value(obj.locationId)

    def deserializeObj(source: DBObjectExtractor) = NewsSubscription(
      locationId = source.field("lid").value[LocationId]
    )
  }
  implicit val NewsUserCommentConverter = new DBObjectConverter[NewsUserComment] {
    def serializeObj(obj: NewsUserComment) = DBObjectBuilder()
      .field("m").value(obj.message)

    def deserializeObj(source: DBObjectExtractor) = NewsUserComment(
      message = source.field("m").value[String]
    )
  }
  implicit val NewsWishConverter = new DBObjectConverter[NewsWish] {
    def serializeObj(obj: NewsWish) = DBObjectBuilder()
      .field("wid").value(obj.wishId)
      .field("lid").value(obj.locationId)
      .field("pid").value(obj.productId)

    def deserializeObj(source: DBObjectExtractor) = NewsWish(
      wishId = source.field("wid").value[WishId],
      locationId = source.field("lid").value[LocationId],
      productId = source.field("pid").value[ProductId]
    )
  }
  implicit val NewsPresentConverter = new DBObjectConverter[NewsPresent] {
    def serializeObj(obj: NewsPresent) = DBObjectBuilder()
      .field("lid").value(obj.locationId)
      .field("pid").value(obj.productId)
      .field("m").value(obj.message)

    def deserializeObj(source: DBObjectExtractor) = NewsPresent(
      locationId = source.field("lid").value[LocationId],
      productId = source.field("pid").value[ProductId],
      message = source.field("m").opt[String]
    )
  }
  implicit val NewsPhotoAlbumConverter = new DBObjectConverter[NewsPhotoAlbum] {
    def serializeObj(obj: NewsPhotoAlbum) = DBObjectBuilder()
      .field("pid").value(obj.photoAlbumId)
      .field("pc").value(obj.photosCount)
      .field("ps").value(obj.photos)

    def deserializeObj(source: DBObjectExtractor) = NewsPhotoAlbum(
      photoAlbumId = source.field("pid").value[PhotoAlbumId],
      photosCount = source.field("pc").value[Int](0),
      photos = source.field("ps").seq[PhotoId]
    )
  }
  implicit val NewsLocationConverter = new DBObjectConverter[NewsOfLocation] {
    protected def serializeObj(obj: NewsOfLocation): DBObjectBuilder = DBObjectBuilder()
      .field("lid").value(obj.locationId)
      .field("m").value(obj.message)

    protected def deserializeObj(source: DBObjectExtractor): NewsOfLocation = NewsOfLocation(
      locationId = source.field("lid").value[LocationId],
      message = source.field("m").value[String])
  }

  implicit val NewsConverter = new DBObjectConverter[News] {
    def serializeObj(obj: News) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uids").value(obj.viewers)
      .field("hus").value(obj.hidedUsers)
      .field("har").value(obj.hidedActorsAndRecipients)
      .field("nt").value(obj.newsType)
      .field("t").value(obj.createdAt)
      .field("aid").value(obj.actorId)
      .field("rid").value(obj.recipientId)
      .field("arid").value(obj.actorsAndRecipientsIds)
      .field("lc").value(obj.likesCount)
      .field("ls").value(obj.usersWhoLike)
      .field("cc").value(obj.commentsCount)
      .field("cs").value(obj.comments)
      .field("ci").value(obj.checkin)
      .field("fl").value(obj.favoriteLocation)
      .field("su").value(obj.subscription)
      .field("uc").value(obj.userComment)
      .field("w").value(obj.wish)
      .field("p").value(obj.present)
      .field("pa").value(obj.photoAlbum)
      .field("ln").value(obj.locationNews)

    def deserializeObj(source: DBObjectExtractor) = News(
      id = source.id.value[NewsId],
      viewers = source.field("uids").seq[UserId],
      hidedUsers = source.field("hus").seq[UserId],
      hidedActorsAndRecipients = source.field("har").seq[UserId],
      newsType = source.field("nt").value[EventTypeId],
      createdAt = source.field("t").value[Date],
      actorId = source.field("aid").opt[UserId],
      recipientId = source.field("rid").opt[UserId],
      actorsAndRecipientsIds = source.field("arid").seq[UserId],
      likesCount = source.field("lc").value[Int](0),
      usersWhoLike = source.field("ls").seq[UserId],
      commentsCount = source.field("cc").value[Int](0),
      comments = source.field("cs").seq[NewsCommentShort],
      checkin = source.field("ci").opt[NewsCheckin],
      favoriteLocation = source.field("fl").opt[NewsFavoriteLocation],
      subscription = source.field("su").opt[NewsSubscription],
      userComment = source.field("uc").opt[NewsUserComment],
      wish = source.field("w").opt[NewsWish],
      present = source.field("p").opt[NewsPresent],
      photoAlbum = source.field("pa").opt[NewsPhotoAlbum],
      locationNews = source.field("ln").opt[NewsOfLocation]
    )
  }
}