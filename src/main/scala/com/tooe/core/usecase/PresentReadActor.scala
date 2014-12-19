package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.usecase.present.{PresentAdminSearchSortType, PresentDataActor}
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain._
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import com.tooe.core.domain._
import com.tooe.api.service._
import com.tooe.core.usecase.present.PresentDataActor.GetPresent
import com.tooe.core.db.mysql.domain.{Payment, ProductSnapshot}
import java.math.BigInteger
import com.tooe.core.service.PresentAdminSearchParams
import com.tooe.core.usecase.payment.PaymentDataActor
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import org.bson.types.ObjectId
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.util._
import com.tooe.core.util.MediaHelper._
import com.tooe.core.usecase.user.response.{ActorShortItem, ActorItem}
import com.tooe.core.usecase.product.ProductDataActor

object PresentReadActor {
  final val Id = Actors.PresentRead

  case class GetUserEventPresents(ids: Set[PresentId])
  case class GetPresents(userId: UserId, parameters: GetPresentParameters, offsetLimit: OffsetLimit, ctx: RouteContext)
  case class GetSentPresents(userId: UserId, request: SentPresentsRequest, offsetLimit: OffsetLimit, ctx: RouteContext)
  case class GetPresentByCode(code: PresentCode, ctx: RouteContext)
  case class GetPresentComment(id: PresentId)
  case class GetPresentsAdminSearch(request: PresentAdminSearchRequest)
}

class PresentReadActor extends AppActor {

  lazy val presentDataActor = lookup(PresentDataActor.Id)
  lazy val paymentDataActor = lookup(PaymentDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val locationReadActor = lookup(LocationReadActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val productSnapshotDataActor = lookup(ProductSnapshotDataActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)

  import scala.concurrent.ExecutionContext.Implicits.global
  import PresentReadActor._

  def receive = {
    case GetUserEventPresents(ids) => getPresents(ids) map (_ map UserEventPresentItem.apply) pipeTo sender

    case GetPresents(userId, parameters, offsetLimit, ctx) =>
      val result = for {
        (presents, count) <- getPresentsAndCountFtr(userId, parameters, offsetLimit)
        users <- findActorsShort(presents.flatMap(_.actorId).toSet, ctx.lang, Images.Mypresents.Full.Actor.Media).map(_.toMapId(_.userId))
        locations <- findLocations(presents, ctx.lang)
      } yield {
        updateStatisticActor ! UpdateStatisticActor.SetUserNewPresentsCounter(userId, 0)
        GetPresentsResponse(count, presents.map { present =>
          val presentLocation = locations.toMapId(_.id)(present.product.locationId)
          PresentDetailsItem(present, presentLocation, users)
        })
      }
      result pipeTo sender

    case GetPresentByCode(code, ctx) =>
      implicit val lang = ctx.lang
      val result = for {
        present <- getUserPresentByCode(code)
        productSnapshot <- if(!present.isArchived) getProductSnapshot(present) else Future(null)
        product <- if(present.isArchived)  getProductFromMongo(present) else Future(null)
        users <- findActorsShort(present.actorId.toSet, ctx.lang, Images.PresentsAdminSearch.Full.Actor.Media).map(_.toMapId(_.userId))
        location <- getLocation(present.product.locationId)
      } yield {
        val productData = if(!present.isArchived) PresentProductData(productSnapshot) else PresentProductData(product)
        GetPresentByCodeResponse(PresentSearchDetailsItem(present, productData, location, users))
      }
      result pipeTo sender

    case GetPresentComment(id: PresentId) =>
      (presentDataActor ? GetPresent(id)).mapTo[Present].map { present =>
        GetPresentCommentResponse(PresentCommentItem(present))
      } pipeTo sender

    case GetPresentsAdminSearch(request) =>
      val params = PresentAdminSearchParams(request.productName, request.locationId, request.status, request.sort, request.offset)
      val result = for {
        presents <- findPresents(params)
        count <- countPresents(params)
        productSnapshots <- getProductSnapshots(presents.map(_.orderId.bigInteger))
        payments <- getPayments(presents.map(_.orderId))
        productSnapshotsByOrderId = productSnapshots.map(ps => ps.orderId -> ps).toMap
        paymentsByOrderId = payments.map(ps => ps.orderId -> ps).toMap
        presentsWithProductSnapshots = presents.map(p => (p, productSnapshotsByOrderId.get(p.orderId), paymentsByOrderId.get(p.orderId)))
      } yield {
        val (valid, invalid) = presentsWithProductSnapshots.partition {
          case (_, optProduct, optPayment) => optProduct.isDefined && optPayment.isDefined
        }
        val responseItems = valid.collect {
          case (present, Some(productSnapshot), Some(payment)) => GetPresentsAdminSearchResponseItem(present, productSnapshot, payment)
        }
        invalid.foreach {
          case (present, productSnapshot, payment) =>
            if (productSnapshot.isEmpty) {
              log.error(s"Cannot find product snapshot with order id ${present.orderId} for present with id ${present.id.id.toString}. Present skipped.")
            }
            if (payment.isEmpty) {
              log.error(s"Cannot find payment with order id ${present.orderId} for present with id ${present.id.id.toString}. Present skipped.")
            }
        }
        GetPresentsAdminSearchResponse(count, responseItems)
      }
      result.pipeTo(sender)

    case GetSentPresents(userId, request, offsetLimit, ctx) =>
      (for {
        (presents, count) <- (presentDataActor ? PresentDataActor.GetUserSentPresents(userId, request, offsetLimit))
                           .zip(presentDataActor ? PresentDataActor.GetUserSentPresentsCount(userId, request)).mapTo[(Seq[Present], Long)]
        users <- findActorsShort(presents.flatMap(_.userId).toSet, ctx.lang, Images.MySentPresents.Full.Recipient.Media).map(_.toMapId(_.userId))
        locations <- findLocations(presents, ctx.lang)
      } yield {
        GetPresentsSentResponse(count, presents.map { present =>
          val presentLocation = locations.toMapId(_.id)(present.product.locationId)
          PresentSentDetailsItem(present, presentLocation, users)
        })
      }) pipeTo sender
  }


  def getProductFromMongo(present: Present): Future[Product] = {
    productDataActor.ask(ProductDataActor.GetProduct(present.product.productId)).mapTo[Product]
  }

  def getProductSnapshot(present: Present) = {
    (productSnapshotDataActor ? ProductSnapshotDataActor.GetProductSnapshot(present.orderId.bigInteger)).mapTo[ProductSnapshot]
  }

  def getLocation(locationId: LocationId): Future[Location] =
    locationDataActor.ask(LocationDataActor.GetLocation(locationId)).mapTo[Location]

  def getUserPresentByCode(code: PresentCode): Future[Present] =
    (presentDataActor ? PresentDataActor.GetUserPresentByCode(code)).mapTo[Present]

  def findLocations(presents: Seq[Present], lang: Lang): Future[Seq[UserEventLocation]] =
    (locationReadActor ? LocationReadActor.FindUserEventLocations(presents.map(_.product.locationId).toSet, lang, Images.Mypresents.Full.Location.Media)).mapTo[Seq[UserEventLocation]]

  def findActorsShort(userIds: Set[UserId], lang: Lang, imageSize: String): Future[Seq[ActorShortItem]] =
    userReadActor.ask(UserReadActor.FindActorsShort(userIds, lang, imageSize)).mapTo[Seq[ActorShortItem]]

  def getPresentsAndCountFtr(userId: UserId, parameters: GetPresentParameters, offsetLimit: OffsetLimit): Future[(Seq[Present], Long)] = {
    (presentDataActor ? PresentDataActor.GetUserPresents(userId, parameters, offsetLimit))
      .zip(presentDataActor ? PresentDataActor.GetUserPresentsCount(userId, parameters)).mapTo[(Seq[Present], Long)]
  }

  def getPresents(ids: Set[PresentId]): Future[Seq[Present]] =
    (presentDataActor ? PresentDataActor.Find(ids)).mapTo[Seq[Present]]

  def findPresents(params: PresentAdminSearchParams) =
    presentDataActor.ask(PresentDataActor.PresentsAdminSearch(params)).mapTo[Seq[Present]]

  def countPresents(params: PresentAdminSearchParams) =
    presentDataActor.ask(PresentDataActor.PresentsAdminSearchCount(params)).mapTo[Long]

  def getProductSnapshots(ids: Seq[BigInteger]) =
    productSnapshotDataActor.ask(ProductSnapshotDataActor.GetProductSnapshots(ids)).mapTo[Seq[ProductSnapshot]]

  def getPayments(ids: Seq[Payment.OrderId]) =
    paymentDataActor.ask(PaymentDataActor.GetPayments(ids)).mapTo[Seq[Payment]]
}

case class PresentAdminSearchRequest(productName: Option[String], locationId: LocationId, status: Option[PresentStatusId], sort: Option[PresentAdminSearchSortType], offset: OffsetLimit)

case class GetPresentsAdminSearchResponse(presentscount: Long, presents: Seq[GetPresentsAdminSearchResponseItem]) extends SuccessfulResponse

case class GetPresentsAdminSearchResponseItemProduct
(
  id: ObjectId,
  name: String,
  `type`: String,
  media: MediaUrl
  ) extends UnmarshallerEntity

case class GetPresentsAdminSearchResponseItemProductPrice(value: BigDecimal, currency: String) extends UnmarshallerEntity

case class GetPresentsAdminSearchResponseItemPaymentPrice(value: BigDecimal, currency: String) extends UnmarshallerEntity

case class GetPresentsAdminSearchResponseItem
(
  id: ObjectId,
  product: GetPresentsAdminSearchResponseItemProduct,
  code: String,
  creationtime: Date,
  receivedtime: Option[Date],
  status: Option[String],
  productprice: GetPresentsAdminSearchResponseItemProductPrice,
  paymentprice: Option[GetPresentsAdminSearchResponseItemPaymentPrice]

  ) extends UnmarshallerEntity

case object GetPresentsAdminSearchResponseItem {

  def apply(present: Present, productSnapshot: ProductSnapshot, payment: Payment): GetPresentsAdminSearchResponseItem =
    GetPresentsAdminSearchResponseItem(
      id = present.id.id,
      product = GetPresentsAdminSearchResponseItemProduct(
        id = present.product.productId.id,
        name = present.product.productName,
        `type` = present.product.productTypeId.id,
        media = present.getProductMediaUrl(Images.LocationPresents.Full.Product.Media)
      ),
      code = present.code.value,
      creationtime = present.createdAt,
      receivedtime = present.receivedAt,
      status = present.presentStatusId.map(_.id),
      productprice = GetPresentsAdminSearchResponseItemProductPrice(productSnapshot.price, productSnapshot.currency),
      paymentprice = Option(GetPresentsAdminSearchResponseItemPaymentPrice(payment.amount, payment.currencyId))
        .filterNot(pp => pp.value == productSnapshot.price && pp.currency == productSnapshot.currency)

    )

}


case class UserEventPresentItem
(
  @JsonProperty("id") id: PresentId,
  @JsonProperty("name") name: String,
  @JsonProperty("media") media: MediaUrl
  )

object UserEventPresentItem {
  def apply(entity: Present): UserEventPresentItem = UserEventPresentItem(
    id = entity.id,
    name = entity.product.productName,
    media = entity.product.media.map(_.url).asMediaUrl(Images.Userevents.Full.Present.Media, ProductDefaultUrlType)
  )
}

case class GetPresentsResponse(@JsonProperty("presentscount") presentsCount: Long, presents: Seq[PresentDetailsItem]) extends SuccessfulResponse

case class PresentDetailsItem
(
  id: PresentId,
  code: String,
  status: Option[String],
  @JsonProperty("creationtime") creationTime: Date,
  @JsonProperty("expirationtime") expirationTime: Date,
  @JsonProperty("receivedtime") receivedTime: Option[Date],
  message: Option[String],
  product: ProductDetailItem,
  location: UserEventLocation,
  actor: Option[ActorShortItem]
  )

object PresentDetailsItem {
  def apply(present: Present, location: UserEventLocation, users: Map[UserId, ActorShortItem]): PresentDetailsItem =
    PresentDetailsItem(
      id = present.id,
      code = present.code.value,
      status = present.presentStatusId.map(_.id),
      creationTime = present.createdAt,
      expirationTime = present.expiresAt,
      receivedTime = present.receivedAt,
      message = present.message,
      product = ProductDetailItem(present.product, Images.Mypresents.Full.Product.Media),
      location = location,
      actor = present.actorId.map(users)
    )
}

case class GetPresentsSentResponse(@JsonProperty("presentscount") presentsCount: Long, presents: Seq[PresentSentDetailsItem]) extends SuccessfulResponse

case class PresentSentDetailsItem
(
  id: PresentId,
  code: String,
  status: Option[String],
  @JsonProperty("creationtime") creationTime: Date,
  @JsonProperty("expirationtime") expirationTime: Date,
  @JsonProperty("receivedtime") receivedTime: Option[Date],
  message: Option[String],
  product: ProductDetailItem,
  location: UserEventLocation,
  recipient: Option[ActorShortItem]
  )

object PresentSentDetailsItem {
  def apply(present: Present, location: UserEventLocation, users: Map[UserId, ActorShortItem]): PresentSentDetailsItem =
    PresentSentDetailsItem(
      id = present.id,
      code = present.code.value,
      status = present.presentStatusId.map(_.id),
      creationTime = present.createdAt,
      expirationTime = present.expiresAt,
      receivedTime = present.receivedAt,
      message = present.message,
      product = ProductDetailItem(present.product, Images.MySentPresents.Full.Product.Media),
      location = location,
      recipient = present.userId.map(users)
    )
}

case class ProductDetailItem
(
  id: ProductId,
  name: String,
  media: MediaUrl,
  description: Option[String],
  @JsonProperty("type") productTypeId: ProductTypeId
  )

object ProductDetailItem {
  def apply(product: PresentProduct, imageSize: String): ProductDetailItem =
    ProductDetailItem(
      id = product.productId,
      name = product.productName,
      media = product.getMediaUrl(imageSize),
      productTypeId = product.productTypeId,
      description = Option(product.description)
    )
}

case class ProductPrice(value: BigDecimal, currency: CurrencyId)

object ProductPrice {

  def apply(product: PresentProductData): ProductPrice = ProductPrice(product.paymentAmount, CurrencyId(product.paymentCurrency))

}

case class GetPresentByCodeResponse(present: PresentSearchDetailsItem) extends SuccessfulResponse

case class PresentSearchDetailsItemLocation
(
  id: LocationId,
  name: String,
  media: MediaUrl,
  address: LocationAddressItem,
  coords: Coordinates,
  @JsonProperty("activationphone") activationPhone: Option[PhoneShort]
  ) extends UnmarshallerEntity

object PresentSearchDetailsItemLocation {
  def apply(l: Location)(implicit lang: Lang): PresentSearchDetailsItemLocation = PresentSearchDetailsItemLocation(
    l.locationId,
    l.name.localized.getOrElse(""),
    l.getMainLocationMediaUrl(Images.PresentsAdminSearch.Full.Location.Media),
    LocationAddressItem(l.contact.address),
    l.contact.address.coordinates,
    l.contact.activationPhone.map(p => PhoneShort(p))
  )

}

case class PresentSearchDetailsItem(id: PresentId,
                                    code: String,
                                    status: Option[String],
                                    @JsonProperty("creationtime") creationTime: Date,
                                    @JsonProperty("expirationtime") expirationTime: Date,
                                    @JsonProperty("receivedtime") receivedTime: Option[Date],
                                    message: Option[String],
                                    product: ProductSearchDetailItem,
                                    location: PresentSearchDetailsItemLocation,
                                    actor: Option[ActorShortItem],
                                    price: ProductPrice) extends UnmarshallerEntity

object PresentSearchDetailsItem {
  def apply(present: Present, product: PresentProductData, location: Location, users: Map[UserId, ActorShortItem])(implicit lang: Lang): PresentSearchDetailsItem =
    PresentSearchDetailsItem(
      id = present.id,
      code = present.code.value,
      status = present.presentStatusId.map(_.id),
      creationTime = present.createdAt,
      expirationTime = present.expiresAt,
      receivedTime = present.receivedAt,
      message = present.message,
      product = ProductSearchDetailItem(product, present.product),
      location = PresentSearchDetailsItemLocation(location),
      actor = present.actorId.map(users),
      price = ProductPrice(product)
    )
}

case class PresentProductData
(
  productId: ProductId,
  name: String,
  description: Option[String],
  productTypeId: ProductTypeId,
  article: Option[String],
  paymentAmount: BigDecimal,
  paymentCurrency: String
  )

object PresentProductData {
  def apply(product: Product)(implicit lang: Lang): PresentProductData = PresentProductData(
    productId = product.id,
    name = product.name.localized getOrElse "",
    description = product.description.localized,
    productTypeId = product.productTypeId,
    article = product.article,
    paymentAmount = product.price.value,
    paymentCurrency = product.price.currency.id
  )
  def apply(product: ProductSnapshot): PresentProductData = PresentProductData(
    productId = ProductId(new ObjectId(product.productId)),
    name = product.name,
    description = Option(product.descr),
    productTypeId = ProductTypeId(product.product_type),
    article = Option(product.article),
    paymentAmount = product.price,
    paymentCurrency = product.currency
  )
}

case class ProductSearchDetailItem
(
  id: ProductId,
  name: String,
  media: MediaUrl,
  description: Option[String],
  @JsonProperty("type") productTypeId: ProductTypeId,
  article: Option[String]
  )

object ProductSearchDetailItem {
  def apply(product: PresentProductData, presentProduct: PresentProduct): ProductSearchDetailItem =
    ProductSearchDetailItem(
      id = product.productId,
      name = product.name,
      media = presentProduct.getMediaUrl(Images.PresentsAdminSearch.Full.Product.Media),
      productTypeId = product.productTypeId,
      description = product.description,
      article = product.article
    )
}

case class GetPresentCommentResponse(present: PresentCommentItem) extends SuccessfulResponse

case class PresentCommentItem(id: PresentId, @JsonProperty("admcomment") comment: String)

object PresentCommentItem {

  def apply(present: Present): PresentCommentItem =
    PresentCommentItem(
      id = present.id,
      comment = present.adminComment.getOrElse("")
    )

}