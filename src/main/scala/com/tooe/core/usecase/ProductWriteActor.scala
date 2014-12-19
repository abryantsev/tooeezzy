package com.tooe.core.usecase

import com.tooe.core.util.Lang
import com.tooe.core.usecase.location.{PreModerationLocationDataActor, LocationDataActor}
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.domain._
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.api.service.{ExecutionContextProvider, SuccessfulResponse, RouteContext}
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import com.tooe.core.usecase.product.ProductDataActor.UpdateProduct
import scala.concurrent.Future
import com.tooe.core.usecase.admin_user.AdminUserDataActor
import com.tooe.core.usecase.wish.WishDataActor
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.usecase.urls.UrlsWriteActor
import com.tooe.core.usecase.DeleteMediaServerActor.DeletePhotoFile
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.api.validation.{ValidationContext, Validatable}

object ProductWriteActor{
  final val Id = Actors.ProductWrite

  case class SaveProduct(request: SaveProductRequest, adminUserId: AdminUserId, ctx: RouteContext)
  case class DeleteProduct(productId: ProductId)
  case class ChangeProduct(productId: ProductId, request: ProductChangeRequest, lang: Lang)
  case class UpdateAdditionalProductCategory(request: ChangeAdditionalCategoryRequest,
                                             renameParameters: RenameAdditionalCategoryParameters,
                                             ctx: RouteContext)
  case class RemoveAdditionalProductCategory(request: ChangeAdditionalCategoryRequest)

}

class ProductWriteActor extends AppActor with ExecutionContextProvider {

  val presentValidityDays = context.system.settings.config.getInt("constraints.product.present-validity-days")

  import ProductWriteActor._

  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val locationModDataActor = lookup(PreModerationLocationDataActor.Id)
  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val wishDataActor = lookup(WishDataActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val deleteMediaServerActor = lookup(DeleteMediaServerActor.Id)

  def receive = {
    case SaveProduct(request, adminUserId, ctx) =>
      (for{
        location <- getActiveLocationOrByLifeCycleStatuses(request, Seq(LifecycleStatusId.Deactivated))
        admin <- getAdminUser(adminUserId)
        product <- saveProduct(ctx, request, location, admin.companyId.get)
      } yield {
        updateStatisticActor ! UpdateStatisticActor.ChangeLocationProductsCounter(location.id, 1)
        addUrlsForProductMedia(product.id, product.productMedia.map(_.media.url))
        countProductsForLocation(request.locationId).onSuccess {
          case 1 =>
            getLocationMod(request.locationId).foreach {
              locationMod  =>
                updateLocationLifecycleStatus(request.locationId, None)
                updateLocationModLifecycleStatus(locationMod.id, None)
            }
        }
        ProductIdResponse(ProductIdItemResponse(product.id))
      }) pipeTo sender

    case ChangeProduct(productId, request, lang) =>
      getLocation(productId).flatMap {
        location => {
          request.productCategories match {
            case Update(categories) if location.additionalLocationCategories.map(_.id).intersect(categories) != categories =>
              Future.failed(ApplicationException(0, "Request additional product categories don't fit those in location"))
            case _ =>
              getProductMedia(productId, request).flatMap { replacedMedia =>
                productDataActor ! UpdateProduct(ChangeProductParams(location.additionalLocationCategories, productId, request), lang)
                replacedMedia.map { mediaForDelete =>
                  deleteMediaServerActor ! DeletePhotoFile(mediaForDelete.map(m => ImageInfo(m.id, ImageType.product, productId.id)))
                }
                request.media.map { media =>
                  addUrlsForProductMedia(productId, Seq(MediaObjectId(media.imageUrl)))
                }
                Future successful SuccessfulResponse
              }
          }
        } //todo connect productId to ProductChangeRequest
      } pipeTo sender


    case DeleteProduct(productId) =>
      getProduct(productId).flatMap(product => countProductsForLocation(product.location.id).map {
        count =>
          wishDataActor ! WishDataActor.MarkWishesAsDeletedByProduct(productId)
          productDataActor ! ProductDataActor.MarkProductAsDeleted(productId)
          updateStatisticActor ! UpdateStatisticActor.ChangeLocationProductsCounter(product.location.id, -1)
          if (count == 1) {
            getLocationMod(product.location.id).foreach {
              locationMod =>
                updateLocationLifecycleStatus(product.location.id, Option(LifecycleStatusId.Deactivated))
                updateLocationModLifecycleStatus(locationMod.id, Option(LifecycleStatusId.Deactivated))
            }
          }
          SuccessfulResponse
      }).pipeTo(sender)

    case UpdateAdditionalProductCategory(request, renameParams, ctx) =>
      productDataActor ! ProductDataActor.UpdateProductsCategories(request, renameParams.name, ctx)
      sender ! SuccessfulResponse

    case RemoveAdditionalProductCategory(request) =>
      productDataActor ! ProductDataActor.RemoveProductsCategories(request)
      sender ! SuccessfulResponse

  }

  def getProductMedia(productId: ProductId, request: ProductChangeRequest): Future[Option[Seq[MediaObjectId]]] =
    request.media.map { _ =>
      (productDataActor ? ProductDataActor.GetProductMediaUrl(productId)).mapTo[Option[Seq[MediaObjectId]]]
    } getOrElse(Future successful None)

  def saveProduct(ctx: RouteContext, request: SaveProductRequest, location: Location, companyId: CompanyId): Future[Product] = {
    if (location.additionalLocationCategories.map(_.id).intersect(request.addCategories.getOrElse(Nil)) != request.addCategories.getOrElse(Nil)){
      Future.failed(ApplicationException(0, "Request additional product categories don't fit those in location"))
    } else {
        val categoryIdToCategory = location.additionalLocationCategories.map(c => (c.id, c)).toMap
        val additionalCategories = request.addCategories.map(_ map categoryIdToCategory)
  
        productDataActor.ask(ProductDataActor.SaveProduct(
        Product(
          companyId = companyId,
          name = Map(ctx.lang -> request.name),
          description = Map(ctx.lang -> request.description),
          price = request.price,
          discount = request.discount,
          productTypeId = request.productTypeId,
          productAdditionalCategories = additionalCategories,
          regionId = location.contact.address.regionId,
          location = LocationWithName(location.locationId, name = location.name),
          maxAvailabilityCount = request.count,
          availabilityCount = request.count,
          article = request.article,
          additionalInfo = request.additionalInfo.map(ai => Map(ctx.lang -> ai)),
          keyWords = request.keyWords,
          productMedia = request.media.toSeq.map(media => ProductMedia(MediaObject(MediaObjectId(media.imageUrl)))),
          lifeCycleStatusId = request.isActive.flatMap(active => if (active) None else Option(ProductLifecycleId.Deactivated)),
          validityInDays = presentValidityDays
        )
      )).mapTo[Product]
    }
  }

  def getAdminUser(adminUserId: AdminUserId) =
    adminUserDataActor.ask(AdminUserDataActor.FindAdminUser(adminUserId)).mapTo[AdminUser]

  def getProduct(productId: ProductId) =
    productDataActor.ask(ProductDataActor.GetProduct(productId)).mapTo[Product]

  def getLocation(request: SaveProductRequest): Future[Location] = {
    locationDataActor.ask(LocationDataActor.GetLocation(request.locationId)).mapTo[Location]
  }
  def getActiveLocationOrByLifeCycleStatuses(request: SaveProductRequest, statuses: Seq[LifecycleStatusId]): Future[Location] = {
    locationDataActor.ask(LocationDataActor.GetActiveLocationOrByLifeCycleStatuses(request.locationId, statuses)).mapTo[Location]
  }

  def getLocationMod(id: LocationId) =
    locationModDataActor.ask(PreModerationLocationDataActor.FindLocationByLocationId(id)).mapTo[PreModerationLocation]

  def countProductsForLocation(id: LocationId) =
    productDataActor.ask(ProductDataActor.CountProductsForLocation(id)).mapTo[Long]

  def updateLocationLifecycleStatus(id: LocationId, status: Option[LifecycleStatusId]) =
    locationDataActor.ask(LocationDataActor.UpdateLifecycleStatus(id, status)).mapTo[UpdateResult]

  def updateLocationModLifecycleStatus(id: PreModerationLocationId, status: Option[LifecycleStatusId]) =
    locationModDataActor.ask(PreModerationLocationDataActor.UpdateLifecycleStatus(id, status)).mapTo[UpdateResult]

  def addUrlsForProductMedia(id: ProductId, url: Seq[MediaObjectId]) {
    urlsWriteActor ! UrlsWriteActor.AddProductMedia(id, url)
  }

  def getLocation(productId: ProductId): Future[Location] =
    (productDataActor ? ProductDataActor.GetProduct(productId)).mapTo[Product]
      .flatMap(product => locationDataActor.ask(LocationDataActor.GetLocation(product.location.id)).mapTo[Location])

}

case class SaveProductRequest(@JsonProperty("type") productTypeId: ProductTypeId,
                              name: String,
                              article: Option[String],
                              price: Price,
                              discount: Option[Discount],
                              description: String,
                              @JsonProperty("addinfo") additionalInfo: Option[String],
                              @JsonProperty("keywords") keyWords: Option[Seq[String]],
                              media: Option[MediaUrl],
                              @JsonProperty("locationid")locationId: LocationId,
                              count: Option[Int],
                              @JsonProperty("addcategories")addCategories: Option[Seq[AdditionalLocationCategoryId]],
                              @JsonProperty("isactive") isActive: Option[Boolean]
                               ) extends UnmarshallerEntity with Validatable with ProductNameValidator {

  def validate(ctx: ValidationContext) {
    nameValidate(ctx, name)
  }

}

trait ProductNameValidator {

  def nameValidate(ctx: ValidationContext, name: String) {
    if(name.split(" ").size > 15)
      ctx.fail("Name must contain less than 15 words")
  }

}

case class ProductIdResponse(product: ProductIdItemResponse) extends SuccessfulResponse
case class ProductIdItemResponse(id: ProductId)

case class ProductChangeRequest(@JsonProperty("type") productTypeId: Option[ProductTypeId],
                                name: Option[String],
                                article: Unsetable[String],
                                price: Option[PriceChange],
                                discount: Unsetable[DiscountChange],
                                description: Option[String],
                                @JsonProperty("addinfo") additionalInformation: Option[String],
                                keywords: Unsetable[Seq[String]],
                                media: Option[MediaUrl],
                                count: Unsetable[Int],
                                @JsonProperty("addcategories") productCategories: Unsetable[Seq[AdditionalLocationCategoryId]],
                                @JsonProperty("isactive") isActive: Unsetable[Boolean]
                                ) extends SuccessfulResponse with Validatable with ProductNameValidator {
  def validate(ctx: ValidationContext) {
    name.foreach(nameValidate(ctx, _))
  }
}

case class PriceChange(value: BigDecimal, currency: CurrencyId)

case class DiscountChange(percentage: Percent,
                          @JsonProperty("startdate") startDate: Unsetable[Date],
                          @JsonProperty("enddate") endDate: Unsetable[Date])

case class ChangeProductParams(additionalCategories: Seq[AdditionalLocationCategory],
                               productId: ProductId,
                               productTypeId: Option[ProductTypeId],
                               name: Option[String],
                               article: Unsetable[String],
                               price: Option[PriceChange],
                               discount: Unsetable[DiscountChange],
                               description: Option[String],
                               additionalInformation: Option[String],
                               keywords: Unsetable[Seq[String]],
                               media: Option[MediaUrl],
                               count: Unsetable[Int],
                               productCategories: Unsetable[Seq[AdditionalLocationCategoryId]],
                               isActive: Unsetable[Boolean]
                                )

object ChangeProductParams {
  def apply(additionalCategories: Seq[AdditionalLocationCategory], productId: ProductId, request:ProductChangeRequest): ChangeProductParams =
    ChangeProductParams(additionalCategories = additionalCategories,
      productId = productId,
      productTypeId = request.productTypeId,
      name =  request.name,
      article = request.article,
      price = request.price,
      discount = request.discount,
      description = request.description,
      additionalInformation = request.additionalInformation,
      keywords = request.keywords,
      media = request.media,
      count = request.count,
      productCategories = request.productCategories,
      isActive = request.isActive)
}