package com.tooe.core.usecase.wish

import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.WishLikeDataService
import scala.concurrent.Future
import com.tooe.core.domain.{UserId, WishId}
import com.tooe.core.db.mongo.domain.WishLike
import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.api.service.OffsetLimit

object WishLikeDataActor {
  final val Id = Actors.WishLikeData

  case class SelectWishLikes(wishId: WishId, offsetLimit: OffsetLimit)
  case class CountLikes(wishId: WishId)

  case class Save(wishLike: WishLike)
  case class Delete(wishId: WishId, userId: UserId)
  case class UserLikeExists(userId: UserId, wishId: WishId)
}

class WishLikeDataActor extends AppActor{
  val service = BeanLookup[WishLikeDataService]

  import WishLikeDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case SelectWishLikes(wishId, offsetLimit) => Future { service.wishLikes(wishId, offsetLimit) } pipeTo sender
    case Save(wishLike) => Future(service.save(wishLike)) pipeTo sender
    case Delete(wishId, userId) => Future(service.delete(wishId, userId)) pipeTo sender
    case UserLikeExists (userId, wishId) => Future { service.userLikeExists(wishId, userId)} pipeTo sender
    case CountLikes(wishId) => Future { service.likesQty(wishId)} pipeTo sender
  }
}