package com.tooe.core.usecase.wish

import akka.actor.Actor
import akka.pattern.ask
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.core.infrastructure.BeanLookup
import concurrent.Future
import akka.pattern.pipe
import com.tooe.core.service.WishDataService
import com.tooe.core.exceptions.NotFoundException
import com.tooe.core.db.mongo.domain.{User, Wish}
import java.util.Date
import com.tooe.core.domain._
import com.tooe.core.util.DateHelper
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.WishId
import com.tooe.core.domain.UserId
import com.tooe.core.domain.ProductId
import com.tooe.core.usecase.user.UserDataActor

object WishDataActor {
  final val Id = Actors.WishData

  case class GetWish(id: WishId)
  case class SaveWish(wish: Wish)
  case class UpdateWishReason(id: WishId, reasonText: Unsetable[String], reasonDate: Unsetable[Date])
  case class DeleteWish(id: WishId)
  case class SelectWishByProduct(userId: UserId, productId: ProductId)
  case class CountUserWishes(userId: UserId, fulfilledOnly: Boolean, location: Option[LocationId] = None)
  case class SelectWishes(userId: UserId, fulfilledOnly: Boolean = false, offsetLimit: OffsetLimit, locationId: Option[LocationId])
  case class Fulfill(userId: UserId, productId: ProductId)
  case class UpdateLikes(wishId: WishId, userId: UserId)
  case class UpdateDislikes(wishId: WishId, userIds: Seq[UserId])
  @deprecated("for special purposes")
  case class MarkWishesAsDeletedByProduct(productId: ProductId)
  case class FindWishes(ids: Set[WishId])
}

class WishDataActor extends Actor with DefaultTimeout with AppActors {

  lazy val service = BeanLookup[WishDataService]
  lazy val userDataActor = lookup(UserDataActor.Id)

  import scala.concurrent.ExecutionContext.Implicits.global
  import WishDataActor._

  def receive = {
    case FindWishes(ids) => Future {
      service.findWishes(ids)
    } pipeTo sender
    case GetWish(id) => Future {
      service.findOne(id) getOrElse (throw NotFoundException(s"Wish(id=$id) not found"))
    } pipeTo sender

    case SaveWish(wish) => Future {
      service.save(wish)
    } pipeTo sender

    case UpdateWishReason(id, reasonText, reasonDate) => Future {
      service.updateReason(id, reasonText, reasonDate)
    } pipeTo sender

    case DeleteWish(id) => Future {
      service.delete(id)
    } pipeTo sender

    case SelectWishByProduct(userId, productId) => Future {
      service.findByUserAndProduct(userId, productId)
    } pipeTo sender

    case CountUserWishes(userId, fulfilledOnly, loc) => loc match {
      case Some(lid) =>
        Future {
          service.countUserWishesPerLocation(userId, fulfilledOnly, lid)
        } pipeTo sender
      case None => userDataActor.ask(UserDataActor.GetUser(userId)).mapTo[User].map {
        user =>
          import user.statistics._
          if (fulfilledOnly) fulfilledWishesCount
          else wishesCount
      } pipeTo sender
    }

    case SelectWishes(userId, fulfilledOnly, offsetLimit, location) => Future {
      if (location.isDefined)
        service.findByUserAndLocation(userId, location.get, fulfilledOnly, offsetLimit)
      else
        service.find(userId, fulfilledOnly, offsetLimit)
    } pipeTo sender

    case Fulfill(userId, productId) => Future(service.fulfill(userId, productId, DateHelper.currentDate)) pipeTo sender

    case UpdateLikes(wishId, userId) =>
      Future(service.updateUserLikes(wishId, userId))

    case UpdateDislikes(wishId, userIds) =>
      Future(service.updateUserDislikes(wishId, userIds))

    case MarkWishesAsDeletedByProduct(pid) =>
      Future(service.markWishesAsDeleted(pid))
  }
}