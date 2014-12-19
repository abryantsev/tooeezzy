package com.tooe.core.usecase

import com.tooe.core.application.Actors
import java.util.Date
import com.tooe.api.JsonProp
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import com.tooe.core.util.DateHelper
import com.tooe.api.service.SuccessfulResponse
import spray.http.StatusCodes
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.usecase.user.UserDataActor
import wish.WishDataActor
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.domain.Unsetable.Skip
import concurrent.Future

object WishWriteActor {

  final val Id = Actors.WishWrite

  case class MakeWish(wish: NewWishRequest, userId: UserId)
  case class UpdateWish(id: WishId, wishReason: WishReason, userId: UserId)
  case class DeleteWish(id: WishId, userId: UserId)
}

class WishWriteActor extends AppActor {

  lazy val wishDataActor = lookup(WishDataActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val newWriteActor = lookup(NewsWriteActor.Id)

  import scala.concurrent.ExecutionContext.Implicits.global

  import WishWriteActor._
  import WishWriteConverters._

  def receive = {
    case MakeWish(newWishRequest, userId) =>
      val result = for {
        _ <- getWishByProduct(userId, newWishRequest.productId).map(wishOpt => if (wishOpt.isDefined) throw ApplicationException(message = "Wish is already made") else wishOpt)
        product <- getProduct(newWishRequest)
        wish = createWish(userId, product, newWishRequest)
        savedWish <- saveWish(wish)
      } yield {
        updateStatisticActor ! UpdateStatisticActor.ChangeUsersWishesCounter(userId, 1)
        userDataActor ! UserDataActor.AddNewWish(userId, savedWish.id)
        newWriteActor ! NewsWriteActor.AddWishNews(userId, wish.id, product)
        WishCreated(wish = WishIdItem(id = savedWish.id))
      }
      result pipeTo sender

    case UpdateWish(wishId, WishReason(reasonText, reasonDate), userId) =>
      checkWishBelongsToCurrentUser(wishId, userId) map { _ =>
        wishDataActor ! WishDataActor.UpdateWishReason(wishId, reasonText, reasonDate)
        SuccessfulResponse
      } pipeTo sender

    case DeleteWish(wishId, userId) =>
      checkWishBelongsToCurrentUser(wishId, userId) map { wish =>
        wishDataActor ! WishDataActor.DeleteWish(wishId)
        if(wish.isFulfilled)
          updateStatisticActor ! UpdateStatisticActor.ChangeUsersFulfilledWishesCounter(userId, -1)
        else
          updateStatisticActor ! UpdateStatisticActor.ChangeUsersWishesCounter(userId, -1)
        userDataActor ! UserDataActor.RemoveWish(userId, wishId)
        SuccessfulResponse
      } pipeTo sender
  }

  def getWishByProduct(userId: UserId, productId: ProductId) =
    wishDataActor.ask(WishDataActor.SelectWishByProduct(userId, productId)).mapTo[Option[Wish]]

  def saveWish(wish: Wish): Future[Wish] =
    wishDataActor.ask(WishDataActor.SaveWish(wish)).mapTo[Wish]

  def getProduct(newWish: NewWishRequest): Future[Product] =
    productDataActor.ask(ProductDataActor.GetProduct(newWish.productId)).mapTo[Product]

  def checkWishBelongsToCurrentUser(wishId: WishId, userId: UserId) =
    for {
      wish <- wishDataActor.ask(WishDataActor.GetWish(wishId)).mapTo[Wish]
    } yield if (wish.userId == userId) wish else
      throw ApplicationException(statusCode = StatusCodes.Forbidden, message = s"Wish(${wishId.id.toString}) is not yours")
}

case class NewWishRequest
(
  @JsonProp("productid") productId: ProductId,
  @JsonProp("reason") reason: Option[String] = None,
  @JsonProp("reasondate") reasonDate: Option[Date] = None
  ) extends UnmarshallerEntity

case class WishReason
(
  @JsonProp("reason") reason: Unsetable[String] = Skip,
  @JsonProp("reasondate") reasonDate: Unsetable[Date] = Skip
  ) extends UnmarshallerEntity

object WishWriteConverters {
  def createWish(userId: UserId, product: Product, newWish: NewWishRequest): Wish =
    new Wish(
      userId = userId,
      product = ProductRef(
        locationId = product.location.id,
        productId = product.id
      ),
      reason = newWish.reason,
      reasonDate = newWish.reasonDate,
      creationDate = DateHelper.currentDate,
      lifecycleStatus = None
    )
}