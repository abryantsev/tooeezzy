package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.domain.UserId
import com.tooe.core.domain.NewsId

@Document(collection = "news")
case class News
(
  id: NewsId = NewsId(),
  viewers: Seq[UserId],
  hidedUsers: Seq[UserId] = Seq.empty,
  hidedActorsAndRecipients: Seq[UserId] = Seq.empty,
  newsType: NewsTypeId,
  createdAt: Date = new Date(),
  actorId: Option[UserId] = None,
  recipientId: Option[UserId] = None,
  actorsAndRecipientsIds: Seq[UserId],
  likesCount: Int = 0,
  usersWhoLike: Seq[UserId] = Seq.empty,
  commentsCount: Int = 0,
  comments: Seq[NewsCommentShort] = Seq.empty,
  checkin: Option[NewsCheckin] = None,
  favoriteLocation: Option[NewsFavoriteLocation] = None,
  subscription: Option[NewsSubscription] = None,
  userComment: Option[NewsUserComment] = None,
  wish: Option[NewsWish] = None,
  present: Option[NewsPresent] = None,
  photoAlbum: Option[NewsPhotoAlbum] = None,
  locationNews: Option[NewsOfLocation] = None
) {

  def getLocationId: Option[LocationId] = newsType match {
    case NewsTypeId.Checkin => checkin map (_.locationId)
    case NewsTypeId.Wish => wish map (_.locationId)
    case NewsTypeId.FavoriteLocation => favoriteLocation map (_.locationId)
    case NewsTypeId.Subscription => subscription map (_.locationId)
    case NewsTypeId.LocationNews | NewsTypeId.LocationTooeezzyNews => locationNews map (_.locationId)
    case NewsTypeId.Present => present map (_.locationId)
    case _ => None
  }

  def getWishId: Option[WishId] = newsType match {
    case NewsTypeId.Wish => wish map (_.wishId)
    case _ => None
  }
  def getProductId: Option[ProductId] = newsType match {
    case NewsTypeId.Present => present map (_.productId)
    case _ => None
  }

  def getMessage: Option[String] = newsType match {
    case NewsTypeId.Message => userComment map (_.message)
    case NewsTypeId.LocationNews | NewsTypeId.LocationTooeezzyNews => locationNews map (_.message)
    case _ => None
  }
}

case class NewsCheckin(locationId: LocationId)

case class NewsFavoriteLocation(locationId: LocationId)
case class NewsSubscription(locationId: LocationId)
case class NewsUserComment(message: String)
case class NewsWish(wishId: WishId, locationId: LocationId, productId: ProductId)

case class NewsPresent(locationId: LocationId, productId: ProductId, message: Option[String])

object NewsPresent{
  def apply(present: Present): NewsPresent = NewsPresent(
    locationId = present.product.locationId,
    productId = present.product.productId,
    message = present.message
  )
}
case class NewsPhotoAlbum(photoAlbumId: PhotoAlbumId, photosCount:Int, photos: Seq[PhotoId])

case class NewsCommentShort(
                             id: NewsCommentId,
                             parentId: Option[NewsCommentId],
                             createdAt: Date,
                             message: String,
                             authorId: UserId
                             )

case class NewsOfLocation(locationId: LocationId,
                        message: String)