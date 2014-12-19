package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.core.db.mongo.util.{UnmarshallerEntity, HasIdentity}
import com.tooe.core.util.DateHelper
import com.tooe.core.db.mongo.util.HasIdentityFactoryEx
import com.tooe.core.domain.{ProductId, LocationId, UserId, WishId}

@Document(collection = "wish")
case class Wish
(
  id: WishId = WishId(),
  userId: UserId,
  product: ProductRef,
  reason: Option[String] = None,
  reasonDate: Option[Date] = None,
  creationDate: Date = DateHelper.currentDate,
  fulfillmentDate: Option[Date] = None,
  likesCount: Int = 0,
  usersWhoSetLikes: Seq[UserId] = Nil,
  lifecycleStatus: Option[String] = None
  ) {
  def isFulfilled = fulfillmentDate.isDefined
}

case class ProductRef
(
  locationId: LocationId,
  productId: ProductId
  )

sealed trait WishPresentStatus extends HasIdentity with UnmarshallerEntity

object WishPresentStatus extends HasIdentityFactoryEx[WishPresentStatus] {

  object Received extends WishPresentStatus {
    def id = "received"
  }

  object Taken extends WishPresentStatus {
    def id = "taken"
  }

  lazy val values = Seq(Received, Taken)
}
