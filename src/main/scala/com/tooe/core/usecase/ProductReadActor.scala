package com.tooe.core.usecase

import com.tooe.core.usecase.location_category.{LocationCategoryActor, LocationCategoryItem}
import concurrent.Future
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.domain._
import com.tooe.core.usecase.product._
import com.tooe.api.service._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.wish.WishDataActor
import com.tooe.core.service.ProductAdminSearchParams
import com.tooe.core.util.{Images, Lang}
import scala.Some
import com.tooe.core.db.mongo.domain._
import com.tooe.core.usecase.LocationReadActor.GetProductMoreInfoResponseLocation
import com.tooe.core.usecase.region.RegionDataActor
import com.tooe.core.util.MediaHelper._
import scala.util.Try

object ProductReadActor {
  final val Id = Actors.ProductRead

  case class GetProductMoreInfo(productId: ProductId, ctx: RouteContext)

  case class GetProductInfo(productId: ProductId, userId: Option[UserId], productView: ProductView, ctx: RouteContext)

  case class GetProductCategories(region: Option[RegionId], ctx: RouteContext)

  case class GetProductsSearch(request: ProductSearchRequest, regionId: RegionId, offsetLimit: OffsetLimit, lang: Lang)

  case class GetProductsLocationSearch(request: LocationProductSearchRequest, locationId: LocationId, productType: Option[ProductTypeId], offsetLimit: OffsetLimit, lang: Lang)

  case class GetProductsAdminSearch(request: ProductAdminSearchRequest, offsetLimit: OffsetLimit, lang: Lang)

  case class GetWishProductItem(productId: ProductId, lang: Lang)

  case class GetProducts(productIds: Seq[ProductId], lang: Lang, imageDimension: String)

  case class GetMiniProducts(productIds: Seq[ProductId], lang: Lang, imageDimension: String)

}

class ProductReadActor extends AppActor with ExecutionContextProvider {

  import ProductReadActor._

  lazy val locationCategoryActor = lookup(LocationCategoryActor.Id)
  lazy val locationReadActor = lookup(LocationReadActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val wishDataActor = lookup(WishDataActor.Id)
  lazy val regionDataActor = lookup(RegionDataActor.Id)

  def receive = {
    case GetProductInfo(id, currentUserId, view, ctx) =>
      implicit val lang = ctx.lang
      (view match {
        case ProductView.Admin =>
          getProduct(id).map(GetProductInfoAdminResponse(_))
        case ProductView.Full =>
          for {
            product <- getProduct(id)
            productLocationItem <- (locationReadActor ? LocationReadActor.GetProductLocationItem(product.location.id, lang)).mapTo[ProductLocationItem]
            wishOpt <- findUsersWish(currentUserId, product.id)
          } yield GetProductInfoResponse(product, productLocationItem, wishOpt)
      }) pipeTo sender

    case GetProductMoreInfo(productId, ctx) =>
      implicit val lang = ctx.lang
      val result = for {
        product <- getProduct(productId)
        productMoreInfoResponseLocation <- (locationReadActor ? GetProductMoreInfoResponseLocation(product.location.id, lang)).mapTo[ProductMoreInfoResponseLocation]
      } yield ProductMoreInfoResponse(
          ProductMoreInfoResponseData(
            id = productId,
            details = ProductMoreInfoResponseDetails(productMoreInfoResponseLocation)
          )
        )
      result pipeTo sender

    case GetProductsSearch(request, regionId, offsetLimit, lang) =>
      implicit val requestLang = lang
      (for {
        (products, count) <- searchProducts(request, regionId, offsetLimit, lang)
      } yield {
        SearchProductResponse(count, products.map(_.map(SearchProductItem(_, Images.Productssearch.Full.Product.Media))))
      }) pipeTo sender

    case GetProductsLocationSearch(request, locationId, productType, offsetLimit, lang) =>
      implicit val requestLang = lang
      lazy val productSearchFtr = (productDataActor ? ProductDataActor.SearchLocationProducts(request, locationId, productType, offsetLimit, lang)).mapTo[Seq[Product]]
      lazy val productCountSearchFtr = (productDataActor ? ProductDataActor.CountSearchLocationProducts(request, locationId, productType)).mapTo[Long]
      (for {
        (products, count) <- productsSearchFuture(request.entities,productSearchFtr)
          .zip(productsSearchCountFuture(request.entities, productCountSearchFtr))
      } yield {
        SearchProductResponse(count, products.map(_.map(SearchProductItem(_, Images.Locationproductssearch.Full.Product.Media))))
      }) pipeTo sender

    case GetProductsAdminSearch(request, offset, lang) =>
      val params = ProductAdminSearchParams(request,offset,lang)
      countProductsByAdminCriteria(params).zip(findProductsByAdminCriteria(params)).map { case (count: Long, products: Seq[Product]) =>
        implicit val l = lang
        GetProductsAdminSearchResponse(count, products.map(GetProductsAdminSearchResponseItem.apply))
      } pipeTo(sender)

    case GetProductCategories(region, ctx) =>
      getProductCategories(ctx.lang).map(_.sortBy(_.name)).flatMap {
        case items if region.isEmpty => Future successful LocationCategoriesResponse(items.map(item => LocationCategoriesResponseItem(isActive = false)(item)))
        case items if region.isDefined =>
          (regionDataActor ? RegionDataActor.GetRegionLocationCategories(region.get)).mapTo[RegionLocationCategories].map {
            region =>
              val regionCategories = Try(region.statistics.locationCategories).toOption.toSeq.flatten
              val (active, none) = items.partition(item => regionCategories.exists(_.id == item.id.id))
              LocationCategoriesResponse(active.map(LocationCategoriesResponseItem(isActive = true)) ++ none.map(LocationCategoriesResponseItem(isActive = false)))
          }
      } pipeTo sender

    case GetWishProductItem(productId, lang) =>
      productDataActor.ask(ProductDataActor.GetProduct(productId)).mapTo[Product].map {
        product =>
          implicit val l = lang
          WishProductItem(product, Images.Product.Full.Self.Media)
      } pipeTo sender

    case GetProducts(productIds, lang, imageDimension) => findProducts(productIds).map(_.map(WishProductItem(_, imageDimension)(lang))) pipeTo sender

    case GetMiniProducts(productIds, lang, imageDimension) => findMiniProducts(productIds).map(_.map(WishMiniProductItem(_, imageDimension)(lang))) pipeTo sender
  }

  def getProduct(id: ProductId) =
    productDataActor.ask(ProductDataActor.GetProduct(id)).mapTo[Product]

  def findUsersWish(userIdOpt: Option[UserId], productId: ProductId): Future[Option[Wish]] =
    userIdOpt map { userId =>
      (wishDataActor ? WishDataActor.SelectWishByProduct(userId, productId)).mapTo[Option[Wish]]
    } getOrElse (Future successful None)

  def productsSearchFuture(entities: Option[Seq[ProductSearchView]], future: => Future[Seq[Product]]) =
    if(entities.map(_.contains(ProductSearchView.Products)).getOrElse(true))
      future.map(Some(_))
    else
      Future successful None

  def productsSearchCountFuture(entities: Option[Seq[ProductSearchView]], future: => Future[Long]) =
    if(entities.map(_.contains(ProductSearchView.ProductsCount)).getOrElse(true))
      future.map(Some(_))
    else
      Future successful None

  def searchProducts(request: ProductSearchRequest, regionId: RegionId, offsetLimit: OffsetLimit, lang: Lang): Future[(Option[Seq[Product]], Option[Long])] = {
    productsSearchFuture(request.entities, (productDataActor ? ProductDataActor.SearchProducts(request, regionId, offsetLimit, lang)).mapTo[Seq[Product]])
      .zip(productsSearchCountFuture(request.entities, (productDataActor ? ProductDataActor.CountSearchProducts(request, regionId)).mapTo[Long]))
  }

  def findProducts(ids: Seq[ProductId]) = productDataActor.ask(ProductDataActor.GetProducts(ids)).mapTo[Seq[Product]]

  def findMiniProducts(ids: Seq[ProductId]) = productDataActor.ask(ProductDataActor.GetMiniWishItemProducts(ids)).mapTo[Seq[Product]]

  def findProductsByAdminCriteria(params: ProductAdminSearchParams) =
    productDataActor.ask(ProductDataActor.ProductsAdminSearch(params)).mapTo[Seq[Product]]

  def countProductsByAdminCriteria(params: ProductAdminSearchParams) =
    productDataActor.ask(ProductDataActor.ProductsAdminCount(params)).mapTo[Long]

  def getProductCategories(lang: Lang) =
    (locationCategoryActor ? LocationCategoryActor.GetProductCategories(Seq(CategoryField.Id, CategoryField.Name), lang)).mapTo[Seq[LocationCategoryItem]]

}

case class ProductAdminSearchRequest
(
  companyId: CompanyId,
  locationId: Option[LocationId],
  productTypeId: Option[ProductTypeId],
  productName: Option[String],
  sort: Option[ProductAdminSearchSortType]
  )

trait ProductSearchParameters {
  def name: Option[String]
  def currencyId: Option[CurrencyId]
  def pmin: Option[Int]
  def pmax: Option[Int]
  def isSale: Option[Boolean]
  def sort: Option[ProductSearchSortType]
  def entities: Option[Seq[ProductSearchView]]
}

case class ProductSearchRequest
(
  category: Option[LocationCategoryId],
  name: Option[String],
  currencyId: Option[CurrencyId],
  pmin: Option[Int],
  pmax: Option[Int],
  isSale: Option[Boolean],
  sort: Option[ProductSearchSortType],
  entities: Option[Seq[ProductSearchView]]
  ) extends ProductSearchParameters

case class LocationProductSearchRequest
(
  category: Option[AdditionalLocationCategoryId],
  name: Option[String],
  currencyId: Option[CurrencyId],
  pmin: Option[Int],
  pmax: Option[Int],
  isSale: Option[Boolean],
  sort: Option[ProductSearchSortType],
  entities: Option[Seq[ProductSearchView]]
  ) extends ProductSearchParameters

case class GetProductsAdminSearchResponse(productscount: Long, products: Seq[GetProductsAdminSearchResponseItem]) extends SuccessfulResponse

case class GetProductsAdminSearchResponseLocationItem(id: LocationId, name: String)

case class GetProductsAdminSearchResponseItem
(
  id: ProductId,
  name: String,
  location: GetProductsAdminSearchResponseLocationItem,
  count: Option[Int],
  media: MediaUrl
  ) extends UnmarshallerEntity

object GetProductsAdminSearchResponseItem {

  def apply(p: Product)(implicit l: Lang): GetProductsAdminSearchResponseItem =
    GetProductsAdminSearchResponseItem(
      id = p.id,
      name = p.name.localized.getOrElse(""),
      location = GetProductsAdminSearchResponseLocationItem(p.location.id, p.location.name.localized.getOrElse("")),
      count = p.availabilityCount,
      media = p.productMedia.headOption.map(_.media).asMediaUrl(Images.CompanyProducts.Full.Self.Media, ProductDefaultUrlType)
    )
}

case class ProductsSearchResponse(products: Option[Seq[ProductItem]],
                                  locations: Option[Seq[LocationItem]], brands: Option[Seq[BrandItem]]) extends UnmarshallerEntity

case class GetProductInfoAdminResponse(product: GetProductInfoAdminResponseItem) extends SuccessfulResponse

object GetProductInfoAdminResponse {

  def apply(product: Product)(implicit lang: Lang): GetProductInfoAdminResponse =
    GetProductInfoAdminResponse(GetProductInfoAdminResponseItem(
      product.id,
      product.lifeCycleStatusId.fold(Option(true))(_ => Option.empty),
      product.productTypeId.id,
      product.name.localized.getOrElse(""),
      product.description.localized.getOrElse(""),
      product.article,
      product.price,
      product.discount,
      product.maxAvailabilityCount,
      product.additionalInfo.map(_.localized.getOrElse("")),
      product.keyWords,
      product.productMedia.headOption.map(_.media).asMediaUrl(Images.Product.Admin.Self.Media, ProductDefaultUrlType),
      ProductItemLocation(product.location.id, product.location.name.localized.getOrElse("")),
      product.productAdditionalCategories.map(c => c.map(AdditionalLocationCategoryItem(_)))
    ))
}

case class GetProductInfoAdminResponseItem(id: ProductId,
                                           @JsonProperty("isactive") active: Option[Boolean],
                                           @JsonProperty("type") productType: String,
                                           name: String,
                                           description: String,
                                           article: Option[String],
                                           price: Price,
                                           discount: Option[Discount],
                                           count: Option[Int],
                                           @JsonProperty("addinfo") addInfo: Option[String],
                                           keywords: Option[Seq[String]],
                                           media: MediaUrl,
                                           location: ProductItemLocation,
                                           @JsonProperty("addcategories") addCategories: Option[Seq[AdditionalLocationCategoryItem]]) extends UnmarshallerEntity     //todo change string type to valid

case class GetProductInfoResponse(product: GetProductInfoResponseItem) extends SuccessfulResponse

object GetProductInfoResponse {

  def apply(product: Product, productLocationItem: ProductLocationItem, wishOpt: Option[Wish])(implicit lang: Lang): GetProductInfoResponse = {
    val mainMedia = product.productMedia.headOption.map(_.media)
    GetProductInfoResponse(GetProductInfoResponseItem(product.id,
      product.name.localized.getOrElse(""),
      product.description.localized.getOrElse(""),
      product.price,
      sale = ProductItemSale(product),
      location = productLocationItem,
      media = mainMedia.asMediaUrl(Images.Product.Full.Self.Media, ProductDefaultUrlType),
      additionalCategories = product.productAdditionalCategories.map(c => c.map(AdditionalLocationCategoryItem(_))),
      validity = product.validityInDays,
      isWished = wishOpt map (_ => true),
      additionalInfo = product.additionalInfo.flatMap(ai => ai.localized),
      loupeMedia = mainMedia.asMediaUrl(Images.Product.Full.Loup.Media, ProductDefaultUrlType)
    )
    )
  }
}

case class GetProductInfoResponseItem(id: ProductId,
                                      name: String,
                                      desc: String,
                                      price: Price,
                                      sale: Option[ProductItemSale],
                                      location: ProductLocationItem,
                                      media: MediaUrl,
                                      @JsonProperty("addcategories") additionalCategories: Option[Seq[AdditionalLocationCategoryItem]],
                                      validity: Int,
                                      @JsonProperty("iswished") isWished: Option[Boolean],
                                      @JsonProperty("addinfo") additionalInfo: Option[String],
                                      @JsonProperty("loupemedia") loupeMedia: MediaUrl) extends UnmarshallerEntity

case class LocationCategoriesResponse(categories: Seq[LocationCategoriesResponseItem]) extends SuccessfulResponse

case class LocationCategoriesResponseItem(id: Option[String], name: Option[String], @JsonProperty("isactive")isActive: Option[Boolean] = None) extends UnmarshallerEntity
object LocationCategoriesResponseItem {
  def apply(isActive: Boolean)(item: LocationCategoryItem): LocationCategoriesResponseItem =
    LocationCategoriesResponseItem(id = Some(item.id.id), name = Some(item.name), isActive = if (isActive) Some(true) else None)
}

case class LocationCategoriesResponseItemMedia(url: String) extends UnmarshallerEntity

case class ProductMoreInfoResponse(product: ProductMoreInfoResponseData) extends SuccessfulResponse

case class ProductMoreInfoResponseData(id: ProductId, details: ProductMoreInfoResponseDetails)

case class ProductMoreInfoResponseDetails(location: ProductMoreInfoResponseLocation)

//TODO from Titan
case class Brands(brands: Seq[BrandItem]) extends UnmarshallerEntity

case class BrandItem()

case class ProductItem(id: ProductId,
                       name: String,
                       price: Price,
                       sale: Option[ProductItemSale],
                       location: ProductItemLocation,
                       media: MediaUrl)

object ProductItem {
  def apply(lang: Lang)(p: Product): ProductItem = ProductItem(
    id = p.id,
    name = p.name.localized(lang) getOrElse "",
    price = p.price,
    sale = ProductItemSale(p),
    location = ProductItemLocation(p.location.id, p.location.name.localized(lang) getOrElse ""),
    media = p.productMedia.headOption.map(_.media).asMediaUrl(Images.Product.Full.Self.Media, ProductDefaultUrlType)
  )
}

case class ProductItemLocation(id: LocationId, name: String)

case class ProductItemSale(discount: Int, @JsonProperty("new_price") newPrice: Price)

object ProductItemSale {
  def apply(product: Product): Option[ProductItemSale] = product.discount map (_.percent.value) map (percent =>
    ProductItemSale(percent, product.price.withDiscount(percent))
  )
}

case class WishMiniProductItem(id: ProductId,
                               name: String,
                               media: MediaUrl)

object WishMiniProductItem {
  def apply(product: Product, imageDimension: String)(implicit lang: Lang): WishMiniProductItem =
    WishMiniProductItem(product.id,
                        product.name.localized getOrElse "",
                        media = product.productMedia.headOption.map(_.media).asMediaUrl(imageDimension, ProductDefaultUrlType))
}

case class WishProductItem
(
  id: ProductId,
  name: String,
  description: Option[String],
  media: MediaUrl,
  price: Price,
  sale: Option[ProductItemSale]
  )

object WishProductItem {

  def apply(product: Product, imageDimension: String)(implicit lang: Lang): WishProductItem = {
    WishProductItem(
      id = product.id,
      name = product.name.localized getOrElse "",
      description = product.description.localized,
      media = product.productMedia.headOption.map(_.media).asMediaUrl(imageDimension, ProductDefaultUrlType),
      price = product.price,
      sale = ProductItemSale(product)
    )
  }

}

case class SearchProductResponse(@JsonProperty("productscount") count: Option[Long],
                                 products: Option[Seq[SearchProductItem]]) extends SuccessfulResponse

case class SearchProductItem(id: ProductId,
                              name: String,
                              price: Price,
                              sale: Option[ProductItemSale],
                              location: ProductItemLocation,
                              media: MediaUrl,
                              @JsonProperty("typeid") typeId: ProductTypeId)

object SearchProductItem {

  def apply(product: Product, imageDimension: String)(implicit lang: Lang): SearchProductItem =
    SearchProductItem(
      id = product.id,
      name = product.name.localized getOrElse "",
      price = product.price,
      sale = ProductItemSale(product),
      location = ProductItemLocation(product.location.id, product.location.name.localized getOrElse ""),
      media = product.productMedia.headOption.map(_.media).asMediaUrl(imageDimension, ProductDefaultUrlType),
      typeId = product.productTypeId
    )

}