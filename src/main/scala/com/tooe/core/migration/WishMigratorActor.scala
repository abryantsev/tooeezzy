package com.tooe.core.migration

import java.util.Date
import com.tooe.core.usecase._
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.domain.UserId
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.migration.db.domain.IdMapping
import com.tooe.core.domain.ProductId
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.domain.WishId
import com.tooe.core.usecase.NewWishRequest
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.user.{UpdateUserStatistic, UserDataActor}
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.db.mongo.domain
import com.tooe.core.usecase.wish.WishDataActor

object WishMigratorActor {
  val Id = 'wishMigratorActor

  case class LegacyWishLike(uid: Int, creationdate: Date)
  case class LegacyWish(legacyid: Int, userid: Int, productid: Int, reason: Option[String], reasondate: Option[Date], creationdate: Date, fulfillment: Option[Date], likes: Seq[LegacyWishLike]) extends UnmarshallerEntity
}

class WishMigratorActor extends MigrationActor {

  import WishMigratorActor._

  def receive = {
    case lw: LegacyWish =>
      val future = for {
        (wishId, userId) <- wishWrite(lw)
        _ <- wishLikesWrite(lw, wishId)
      } yield MigrationResponse(DefaultMigrationResult(lw.legacyid, wishId.id, "wish_migrator"))
      future pipeTo sender
  }

  def wishWrite(lw: LegacyWish): Future[(WishId, UserId)] = {
    for {
      productId <- lookupByLegacyId(lw.productid, MappingCollection.product).map(ProductId)
      product <- (productDataActor ? ProductDataActor.GetProduct(productId)).mapTo[domain.Product]
      userId <- lookupByLegacyId(lw.userid, MappingCollection.user).map(UserId)
      wish = createWish(userId, product, NewWishRequest(productId, lw.reason), lw)
      savedWish <- wishDataActor ? WishDataActor.SaveWish(wish)
      _ <- idMappingDataActor ? IdMappingDataActor.SaveIdMapping(IdMapping(new ObjectId(), MappingCollection.wish, lw.legacyid, wish.id.id))
    } yield {
      if (lw.fulfillment.isEmpty)
        userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(wishes = Some(1)))
      else
        userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(fulfilledWishes = Some(1)))
      userDataActor ! UserDataActor.AddNewWish(userId, wish.id)
      (wish.id, userId)
    }
  }

  def wishLikesWrite(lw: LegacyWish, wishId: WishId): Future[Seq[Any]] = {
    getIdMappings(lw.likes.map(_.uid), MappingCollection.user).map(_.zip(lw.likes)).mapInner {
      case (id, ll) =>
        wishLikeWriteActor ? WishLikeWriteActor.SaveMigrationLike(wishId, UserId(id), ll.creationdate)
    }
  }

  def createWish(userId: UserId, product: domain.Product, newWish: NewWishRequest, lw: LegacyWish): domain.Wish =
    new domain.Wish(
      userId = userId,
      product = domain.ProductRef(
        locationId = product.location.id,
        productId = product.id
      ),
      likesCount = 0,
      fulfillmentDate = lw.fulfillment,
      reason = newWish.reason,
      reasonDate = newWish.reasonDate,
      creationDate = lw.creationdate,
      lifecycleStatus = None
    )

  lazy val wishLikeWriteActor = lookup(WishLikeWriteActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val wishDataActor = lookup(WishDataActor.Id)
}
