package com.tooe.core.usecase.product

import akka.actor.{ActorLogging, Actor}
import com.tooe.core.util.{Lang, ActorHelper}
import com.tooe.core.infrastructure.BeanLookup
import concurrent.Future
import com.tooe.core.application.{Actors, AppActors}
import akka.pattern.pipe
import com.tooe.core.service.{ProductAdminSearchParams, ProductDataService}
import com.tooe.core.domain._
import com.tooe.core.usecase._
import com.tooe.core.db.mongo.domain.Product
import com.tooe.api.service.{RouteContext, OffsetLimit}
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object ProductDataActor {
  final val Id = Actors.ProductData

  case class GetProduct(productId: ProductId)
  case class SaveProduct(entity: Product)
  case class ChangeAvailability(id: ProductId, amount: Int)
  case class SearchProducts(request: ProductSearchRequest, regionId: RegionId, offsetLimit: OffsetLimit, lang: Lang)
  case class CountSearchProducts(request: ProductSearchRequest, regionId: RegionId)
  case class SearchLocationProducts(request: LocationProductSearchRequest, locationId: LocationId, productType: Option[ProductTypeId], offsetLimit: OffsetLimit, lang: Lang)
  case class CountSearchLocationProducts(request: LocationProductSearchRequest, locationId: LocationId, productType: Option[ProductTypeId])
  case class ProductsAdminSearch(params: ProductAdminSearchParams)
  case class ProductsAdminCount(params: ProductAdminSearchParams)
  case class UpdateProduct(changeProductParams: ChangeProductParams, lang: Lang)
  case class GetProducts(productIds: Seq[ProductId])
  case class IncreasePresentCounter(productId: ProductId, delta: Int)
  case class GetMiniWishItemProducts(productIds: Seq[ProductId])
  case class MarkProductAsDeleted(productId: ProductId)
  case class UpdateProductsCategories(request: ChangeAdditionalCategoryRequest, newCategoryName: String, ctx: RouteContext)
  case class RemoveProductsCategories(request: ChangeAdditionalCategoryRequest)
  case class GetProductMediaUrl(id: ProductId)
  case class CountProductsForLocation(id: LocationId)

}

class ProductDataActor extends Actor with ActorHelper with ActorLogging with AppActors {

  lazy val productService = BeanLookup[ProductDataService]

  import scala.concurrent.ExecutionContext.Implicits.global
  import ProductDataActor._

  def receive = {
    case GetProduct(id) => Future {
      productService.findOne(id.id).getOrNotFoundException("Product " + id.id + " not found")
    } pipeTo sender

    case SearchProducts(request, regionId, offsetLimit, lang) =>
      Future(productService.searchByRegion(request, regionId, offsetLimit, lang)) pipeTo sender

    case CountSearchProducts(request, regionId) =>  Future(productService.countSearchByRegion(request, regionId)) pipeTo sender

    case ProductsAdminSearch(params) =>
      Future(productService.findByAdminCriteria(params)) pipeTo sender

    case ProductsAdminCount(params) =>
      Future(productService.countByAdminCriteria(params)) pipeTo sender

    case SaveProduct(product) => Future { productService.save(product) } pipeTo sender

    case ChangeAvailability(id, amount) => Future { productService.changeAvailability(id, amount) } pipeTo sender

    case UpdateProduct(changeProductParams, lang) => Future {
      productService.update(changeProductParams, lang)
    }

    case GetProducts(productIds) => Future {productService.findByIds(productIds)} pipeTo sender

    case IncreasePresentCounter(productId, delta) =>
      Future(productService.increasePresentCounter(productId, delta)) pipeTo sender

    case GetMiniWishItemProducts(productIds) => Future {productService.miniWishItemProductsByIds(productIds)} pipeTo sender

    case SearchLocationProducts(request, locationId, productType, offsetLimit, lang) =>
      Future(productService.searchForLocation(request, locationId, productType, offsetLimit, lang)) pipeTo sender

    case CountSearchLocationProducts(request, locationId, productType) =>
      Future(productService.countSearchForLocation(request, locationId, productType)) pipeTo sender

    case MarkProductAsDeleted(productId) =>
      Future(productService.markProductAsDeleted(productId))

    case UpdateProductsCategories(request, newCategoryName, ctx) =>
      Future(productService.updateAdditionalProductsCategories(request.locationId, request.categoryId, newCategoryName, ctx.lang))

    case RemoveProductsCategories(request) =>
      Future(productService.removeAdditionalProductsCategory(request.locationId, request.categoryId))

    case CountProductsForLocation(id) => Future(productService.countProductsForLocation(id)).pipeTo(sender)

    case GetProductMediaUrl(id) => Future {  productService.getMedia(id) } pipeTo sender

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { productService.updateMediaStorageToS3(ProductId(msg.url.entityId), msg.url.mediaId, msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { productService.updateMediaStorageToCDN(ProductId(msg.url.entityId), msg.url.mediaId) }
  }
}