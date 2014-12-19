package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.domain.{UserId, WishId}
import com.tooe.core.db.mongo.domain.WishLike
import java.util.Date
import com.tooe.core.usecase.wish.WishLikeDataActor.Save
import com.tooe.core.usecase.wish.{WishDataActor, WishLikeDataActor}
import com.tooe.core.usecase.wish.WishDataActor.UpdateLikes
import com.tooe.api.service.{SuccessfulResponse, OffsetLimit}
import akka.pattern.{ask, pipe}


object WishLikeWriteActor {

  final val Id = Actors.WishLikeWrite

  case class SaveLike(wishId: WishId, userId: UserId)
  case class DislikeWish(wishId: WishId, userId: UserId)
  case class SaveMigrationLike(wishId: WishId, userId: UserId, creationTime: Date)

}

class WishLikeWriteActor extends AppActor {
  lazy val wishLikeDataActor = lookup(WishLikeDataActor.Id)
  lazy val wishDataActor = lookup(WishDataActor.Id)

  import scala.concurrent.ExecutionContext.Implicits.global
  import WishLikeWriteActor._

  def receive = {
    case SaveLike(wishId, userId) =>
      val like = WishLike(wishId = wishId, created = new Date, userId = userId)
      (wishLikeDataActor ? Save(like)).onSuccess {
        case _ =>
          wishDataActor ! UpdateLikes(wishId, userId)
      }
      sender ! SuccessfulResponse

    case DislikeWish(wishId, userId) =>
      for {
        _ <- wishLikeDataActor ? WishLikeDataActor.Delete(wishId, userId)
        lastLikes <- (wishLikeDataActor ? WishLikeDataActor.SelectWishLikes(wishId, OffsetLimit(0,20))).mapTo[Seq[WishLike]]
      } yield {
         wishDataActor ! WishDataActor.UpdateDislikes(wishId, lastLikes map (_.userId))
      }
      sender ! SuccessfulResponse

    case SaveMigrationLike(wishId, userId, creationTime) =>
      val like = WishLike(wishId = wishId, created = creationTime, userId = userId)
      (wishLikeDataActor ? Save(like)).onSuccess {
        case _ =>
          wishDataActor ! UpdateLikes(wishId, userId)
      }
      sender ! SuccessfulResponse
  }
}
