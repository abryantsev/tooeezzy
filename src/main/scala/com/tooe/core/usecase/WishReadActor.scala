package com.tooe.core.usecase

import com.tooe.core.application.Actors
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.api.JsonProp
import com.tooe.core.db.mongo.domain
import com.tooe.core.usecase.wish.{WishLikeDataActor, WishDataActor}
import com.tooe.core.domain._
import com.tooe.core.util.{Images, Lang}
import com.tooe.api.service.{SuccessfulResponse, OffsetLimit}
import scala.concurrent.Future
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.WishId
import com.tooe.core.domain.UserId
import com.tooe.core.domain.ProductId
import com.tooe.core.usecase.user.response.Author
import com.tooe.core.db.mongo.domain.Wish

object WishReadActor {
  final val Id = Actors.WishRead

  case class GetWish(id: WishId, viewType: ViewTypeEx, lang: Lang, currentUserId: UserId)
  case class SelectWishes(userId: UserId, params: SelectParams, currentUserId: UserId, locId: Option[LocationId])
  case class SelectCurrentUserWishes(userId: UserId, params: SelectParams)
  case class SelectParams(fulfilledOnly: Boolean = false, viewType: ViewTypeEx, offsetLimit: OffsetLimit, lang: Lang)
}

class WishReadActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import WishReadActor._

  lazy val wishDataActor = lookup(WishDataActor.Id)
  lazy val productReadActor = lookup(ProductReadActor.Id)
  lazy val locationReadActor = lookup(LocationReadActor.Id)
  lazy val locationActor = lookup(LocationReadActor.Id)
  lazy val wishLikeDataActor = lookup(WishLikeDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)

  def receive = {
    case GetWish(id, viewType, lang, currentUserId) =>
      val result = for {
        wish <- wishDataActor.ask(WishDataActor.GetWish(id)).mapTo[domain.Wish]
        wishResponse <- getWishItem(wish.userId, viewType, lang, currentUserId = currentUserId, Images.Wish.Full.Location.Media)(wish)
      } yield OneWishResponse(wishResponse)
      result pipeTo sender

    case SelectWishes(userId, params, currentUserId, locationId) => selectWishes(userId, params, currentUserId = currentUserId, imageDimension = imageLocationDimension(params.viewType), loc = locationId) pipeTo sender

    case SelectCurrentUserWishes(userId, params) => selectWishes(userId, params, currentUserId = userId, getRemoved = true, imageLocationDimension(params.viewType), loc = None) pipeTo sender
  }

  def imageLocationDimension(viewType: ViewTypeEx) = viewType match {
    case ViewTypeEx.None =>  Images.Wishes.Full.Location.Media
    case ViewTypeEx.Short =>  Images.Wishes.Short.Location.Media
    case ViewTypeEx.Mini =>  Images.Wishes.Mini.Location.Media
  }

  def selectWishes(userId: UserId, params: SelectParams, currentUserId: UserId, getRemoved: Boolean = false, imageDimension: String, loc: Option[LocationId]) = {

    import params._

    lazy val wishesFtr = wishDataActor.ask(WishDataActor.SelectWishes(userId, fulfilledOnly, offsetLimit, loc)).mapTo[Seq[domain.Wish]].map(wishes =>
      if (getRemoved) wishes else wishes.filterNot(_.lifecycleStatus.exists(_ == "r")))

    lazy val wishesCount = if (params.offsetLimit.offset == params.offsetLimit.defaultOffset) {
      wishDataActor.ask(WishDataActor.CountUserWishes(userId, fulfilledOnly, loc)).mapTo[Int].map(Some.apply)
    } else Future successful None

    lazy val fullWishItemsFtr = wishesFtr flatMap (w => getWishItems(userId, w, viewType, currentUserId = currentUserId, imageDimension)(lang))

    lazy val miniWishItemsFtr = wishesFtr flatMap (w => getMiniWishItems(userId, w, viewType, currentUserId = currentUserId)(lang))

    def miniResponse = for {
        wishes <- miniWishItemsFtr
        count <- wishesCount
      } yield MiniWishesResponse(count, wishes)


    def shortResponse = for {
      items <- fullWishItemsFtr
      count <- wishesCount
    } yield WishesResponse(count, items)

    def fullResponse = for {
      count <- wishesCount
      items <- fullWishItemsFtr
    } yield WishesResponse(count, items)

    val future: Future[SuccessfulResponse] = params.viewType match {
      case ViewTypeEx.Mini => miniResponse
      case ViewTypeEx.Short => shortResponse
      case ViewTypeEx.None => fullResponse
    }

    future pipeTo sender
  }

  def getWishItem(userId: UserId, viewType: ViewTypeEx, lang: Lang, currentUserId: UserId, locationImageDimension: String)(wish: domain.Wish) =
    getWishItems(userId, Seq(wish), viewType, currentUserId, locationImageDimension)(lang) map (_.head)

  def getSelfLiked(uid: UserId, wishId: WishId) =
    wishLikeDataActor.ask(WishLikeDataActor.UserLikeExists(uid, wishId)).mapTo[Boolean]

  def getMiniWishItems(userId: UserId, wishes: Seq[Wish], viewType: ViewTypeEx, currentUserId: UserId)(implicit lang: Lang): Future[Seq[MiniWishItem]] = {
    val productMapFtr = {
      val productIds = wishes.map(_.product.productId.id).toSet
      findMiniProducts(productIds, Images.Wishes.Mini.Product.Media).map(_.toMapId(_.id))
    }
    productMapFtr.map(m => wishes.map(w => MiniWishItem(m)(w)))
  }

  def productImageFromViewType(viewType: ViewTypeEx) = viewType match {
    case ViewTypeEx.None =>  Images.Wishes.Full.Product.Media
    case ViewTypeEx.Short =>  Images.Wishes.Short.Product.Media
    case ViewTypeEx.Mini =>  Images.Wishes.Mini.Product.Media
  }

  def getWishItems(userId: UserId, wishes: Seq[Wish], viewType: ViewTypeEx, currentUserId: UserId, imageDimension: String)(implicit lang: Lang): Future[Seq[WishItem]] = {
    val productMapFtr = {
      val productIds = wishes.map(_.product.productId.id).toSet
      findProducts(productIds, productImageFromViewType(viewType)).map(_.toMapId(_.id))
    }

    val locationMapFtr = {
      val locationIds = wishes.map(w => LocationId(w.product.locationId.id)).toSet
      findLocations(locationIds, imageDimension) map (_.toMapId(_.id))
    }

    val authorMapFtr = {
      val future = {
        val authorIds = wishes.flatMap(_.usersWhoSetLikes.takeRight(10))
        userReadActor.ask(UserReadActor.SelectAuthors(authorIds, Images.Wishes.Full.Like.Media)).mapTo[Seq[Author]]
      }
      future.map(_.toMapId(_.id))
    }

    for {
      productMap <- productMapFtr
      locationMap <- locationMapFtr
      authorMap <- authorMapFtr
    } yield wishes map WishItem(productMap, locationMap, authorMap, viewType, userId, currentUserId = currentUserId)
  }

  def findProducts(ids: Set[ObjectId], imageDimension: String)(implicit lang: Lang): Future[Seq[WishProductItem]] =
    (productReadActor ? ProductReadActor.GetProducts(ids.toSeq.map(ProductId), lang, imageDimension)).mapTo[Seq[WishProductItem]]

  def findMiniProducts(ids: Set[ObjectId], imageDimension: String)(implicit lang: Lang): Future[Seq[WishMiniProductItem]] =
    (productReadActor ? ProductReadActor.GetMiniProducts(ids.toSeq.map(ProductId), lang, imageDimension)).mapTo[Seq[WishMiniProductItem]]

  def findLocations(ids: Set[LocationId], imageDimension: String)(implicit lang: Lang): Future[Seq[UserEventLocation]] =
    (locationActor ? LocationReadActor.FindUserEventLocations(ids, lang, imageDimension)).mapTo[Seq[UserEventLocation]]

}

case class MiniWishesResponse(@JsonProp("wishescount") wishesCount: Option[Int], @JsonProp("wishes") wishes: Seq[MiniWishItem]) extends SuccessfulResponse

case class WishesResponse(@JsonProp("wishescount") wishesCount: Option[Int],
                          @JsonProp("wishes") wishes: Seq[WishItem]) extends SuccessfulResponse

case class MiniWishItem(id: WishId,
                        product: WishMiniProductItem)

object MiniWishItem {
  def apply(productMap: Map[ProductId, WishMiniProductItem])(wish: Wish): MiniWishItem =
    MiniWishItem(id = wish.id,
      product = productMap(wish.product.productId))
}

case class WishItem
(
  id: WishId,
  fulfilled: Boolean,
  reason: Option[String] = None,
  time: Option[Date] = None,
  product: WishProductItem,
  location: UserEventLocation,
  @JsonProp("likescount") likesCount: Option[Int] = None,
  likes: Option[Seq[WishLikeItem]] = None,
  @JsonProp("selfliked") selfLiked: Option[Boolean],
  @JsonProp("lifecycle") lifeCycle: Option[String]
  )

object WishItem {

  def apply(wish: domain.Wish,
            product: WishProductItem,
            location: UserEventLocation,
            viewType: ViewTypeEx,
            likes: Seq[WishLikeItem],
            selfLiked: Boolean)(implicit lang: Lang): WishItem = {
    val shortWish = WishItem(
      id = wish.id,
      fulfilled = wish.fulfillmentDate.isDefined,
      reason = wish.reason,
      time = wish.reasonDate,
      product = product,
      location = location,
      selfLiked = if (selfLiked) Some(selfLiked) else None,
      lifeCycle = wish.lifecycleStatus
    )
    import com.tooe.core.util.SomeWrapper._

    if (viewType == ViewTypeEx.Short) shortWish
    else shortWish.copy(
      likesCount = wish.likesCount,
      likes = likes
    )
  }

  def apply(productMap: Map[ProductId, WishProductItem],
            locationMap: Map[LocationId, UserEventLocation],
            authorMap: Map[UserId, Author],
            viewType: ViewTypeEx,
            userId: UserId,
            currentUserId: UserId)(wish: Wish): WishItem = {

    val authors = wish.usersWhoSetLikes.takeRight(10).map(authorMap)
    val selfliked = authors.map(_.id).contains(currentUserId)
    val shortWish = WishItem(
      id = wish.id,
      fulfilled = wish.fulfillmentDate.isDefined,
      reason = wish.reason,
      time = wish.reasonDate,
      product = productMap(wish.product.productId),
      location = locationMap(wish.product.locationId),
      selfLiked = if (selfliked)
        Some(true)
      else
        None,
      lifeCycle = wish.lifecycleStatus
    )

    import com.tooe.core.util.SomeWrapper._

    val wishLikeItems = authors.map(WishLikeItem)
    if (viewType == ViewTypeEx.Short) shortWish
    else shortWish.copy(
      likesCount = wish.likesCount,
      likes = wishLikeItems
    )
  }

}

case class WishCreated(wish: WishIdItem) extends SuccessfulResponse

case class WishIdItem(id: WishId)

case class OneWishResponse(wish: WishItem) extends SuccessfulResponse

case class WishLikeItem(author: user.response.Author)
