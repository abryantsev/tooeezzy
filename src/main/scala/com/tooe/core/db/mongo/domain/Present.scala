package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import com.tooe.core.util.MediaHelper._
import com.tooe.core.domain._
import com.tooe.core.db.mysql.domain.Payment

@Document(collection = "present")
case class Present
(
  id: PresentId,
  code: PresentCode,
  userId: Option[UserId],
  anonymousRecipient: Option[AnonymousRecipient],
  senderId: UserId,
  hideSender: Option[Boolean],
  message: Option[String],
  createdAt: Date,
  expiresAt: Date,
  receivedAt: Option[Date] = None,
  presentStatusId: Option[PresentStatusId] = None,
  product: PresentProduct,
  orderId: Payment.OrderId,
  adminComment: Option[String],
  lifeCycle: Option[PresentLifecycleId] = None,
  hideForUsers: Seq[UserId] = Nil
) {
  def actorId: Option[UserId] = if (hideSender getOrElse false) None else Some(senderId)

  def getProductMediaUrl(imageSize: String) = product.getMediaUrl(imageSize)
  def isArchived = lifeCycle.exists(_.id == PresentLifecycleId.Archived.id)
  def isReceived = receivedAt.isDefined || presentStatusId == Some(PresentStatusId.received)
}

case class AnonymousRecipient(phone: Option[PhoneShort], email: Option[String])

case class PresentProduct
(
  companyId: CompanyId,
  locationId: LocationId,
  productId: ProductId,
  productName: String,
  description: String,
  media: Option[PresentProductMedia],
  productTypeId: ProductTypeId
) {
  def getMediaUrl(imageSize: String) = media.map(_.url).asMediaUrl(imageSize, ProductDefaultUrlType)
}

case class PresentProductMedia
(
  url: MediaObject,
  description: Option[String] = None
  )