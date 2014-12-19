package com.tooe.core.service

import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.ProductRepository
import com.tooe.core.db.mongo.domain._
import scala.collection.JavaConverters._
import com.tooe.core.domain._
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.util.{Lang, BuilderHelper}
import java.util.Date
import com.tooe.core.db.mongo.query._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.usecase.product.{ProductAdminSearchSortType, ProductSearchSortType}
import com.tooe.core.db.mongo.converters.{ProductConverter, DBCommonConverters, DBObjectConverters}
import com.tooe.core.usecase._
import java.util.regex.Pattern
import com.mongodb.BasicDBObject
import org.springframework.stereotype.Service
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.db.mongo.query.LocalizedField
import com.tooe.core.domain.Unsetable.{Update => UnsetableUpdate, _}


trait ProductDataService extends DataService[Product, ObjectId] {
  def searchByRegion(request: ProductSearchRequest, regionId: RegionId, offsetLimit: OffsetLimit, lang: Lang): Seq[Product]

  def countSearchByRegion(request: ProductSearchRequest, regionId: RegionId): Long

  def findByAdminCriteria(params: ProductAdminSearchParams): Seq[Product]

  def countByAdminCriteria(params: ProductAdminSearchParams): Long

  def changeAvailability(id: ProductId, amount: Int): UpdateResult

  def update(changeProductParams: ChangeProductParams, lang: Lang): Unit

  def findByIds(productIds: Seq[ProductId]): Seq[Product]

  def increasePresentCounter(productId: ProductId, delta: Int): UpdateResult

  def miniWishItemProductsByIds(productIds: Seq[ProductId]): Seq[Product]

  def markProductAsDeleted(productId: ProductId): Unit

  def updateAdditionalProductsCategories(locationId: LocationId, categoryId: AdditionalLocationCategoryId, newCategoryName: String, lang: Lang)

  def removeAdditionalProductsCategory(locationId: LocationId, categoryId: AdditionalLocationCategoryId)

  def searchForLocation(request: LocationProductSearchRequest, locationId: LocationId, productType: Option[ProductTypeId], offsetLimit: OffsetLimit, lang: Lang): Seq[Product]

  def countSearchForLocation(request: LocationProductSearchRequest, locationId: LocationId, productType: Option[ProductTypeId]): Long

  def updateMediaStorageToS3(productId: ProductId, media: MediaObjectId, newMedia: MediaObjectId): Unit

  def updateMediaStorageToCDN(productId: ProductId, media: MediaObjectId): Unit

  def getMedia(productId: ProductId): Option[Seq[MediaObjectId]]

  def countProductsForLocation(id: LocationId): Long

  case class SearchLocationProducts(request: ProductSearchRequest, locationId: LocationId, offsetLimit: OffsetLimit, lang: Lang)
  case class CountSearchLocationProducts(request: ProductSearchRequest, locationId: LocationId)

}

case class ProductSearchParams
(
  category: Option[String],
  productCategory: Option[AdditionalLocationCategoryId],
  name: Option[String],
  isSale: Option[Boolean],
  saleDate: Option[Date],
  lang: Lang,
  offsetLimit: OffsetLimit,
  sort: Option[ProductSearchSortType]
  )

case class ProductAdminSearchParams
(
  companyId: CompanyId,
  locationId: Option[LocationId],
  productTypeId: Option[ProductTypeId],
  productName: Option[String],
  lang: Lang,
  offsetLimit: OffsetLimit,
  sort: Option[ProductAdminSearchSortType]
  )

object ProductAdminSearchParams
{
  def apply(request: ProductAdminSearchRequest, offset: OffsetLimit, lang: Lang): ProductAdminSearchParams =
    ProductAdminSearchParams(request.companyId, request.locationId, request.productTypeId, request.productName, lang, offset, request.sort)
}

@Service
class ProductDataServiceImpl extends ProductDataService with DataServiceImpl[Product, ObjectId] with ProductConverter{
  @Autowired var wishService: WishDataService = _
  @Autowired var repo: ProductRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[Product]

  import BuilderHelper._
  import DBObjectConverters._
  import DBCommonConverters._
  import com.tooe.core.util.ProjectionHelper._

  implicit def filteringCriteriaHelper(criteria: Criteria) = new {
    def filteringCriteria(params: ProductSearchParams) = {
      import params._
      criteria.extendIf(_.and("pc").all(category.get), category.isDefined)
        .extendIf(_.and("pac").all(productCategory.map(_.id).get), productCategory.isDefined)
        .extendIf(_.and("di.pd").gt(0).lte(100), isSale.isDefined)
        .extendIf(_.andOperator(
        new Criteria("di.st").lte(saleDate.get),
        new Criteria().orOperator(
          new Criteria("di.et").gt(saleDate.get),
          new Criteria("di.et").is(null)
        )
      ), saleDate.isDefined)
        .extendIf(_.and("n." + lang.id).is(name.get), name.isDefined)
    }

    def filteringCriteria(params: ProductAdminSearchParams): Criteria = {
      val criteria: Criteria = Criteria.where("cid").is(params.companyId.id)
        .extend(params.productName)(name => _.and("ns").regex(s"^${name.toLowerCase}"))
        .and("lfs").exists(false)

      criteria.applyIfDefined(params.locationId)((c, lid) => c.and("l.lid").is(lid.id))
        .applyIfDefined(params.productTypeId)((c, ptid) => c.and("pt").is(ptid.id))
    }
  }

  def searchByRegion(request: ProductSearchRequest, regionId: RegionId, offsetLimit: OffsetLimit, lang: Lang) = {
    val query = Query.query(searchProductByRegionCriteria(request, regionId))
      .withPaging(offsetLimit)
      .sort(request.sort.toSort(lang))
    query.fields().exclude("ns")
    mongo.find(query, entityClass).asScala.toSeq
  }

  def countSearchByRegion(request: ProductSearchRequest, regionId: RegionId) = mongo.count(Query.query(searchProductByRegionCriteria(request, regionId)), entityClass)

  private[this] def searchProductByRegionCriteria(request: ProductSearchRequest, regionId: RegionId ) = {
    searchProductCriteria(request).and("rid").is(regionId.id).extend(request.category)(category => _.and("pc").in(category.id))
  }

  private[this] def searchProductCriteria(request: ProductSearchParameters) = {
      new Criteria()
        //note: we cannot add in query criteria by field twice (sample .and("a") ... .and("a") ...)
        .extend(request.pmax.isDefined && request.pmin.isEmpty)(_.extend(request.pmax)(max => _.and("p.v").lte(max / 100)))
        .extend(request.pmin.isDefined && request.pmax.isEmpty)(_.extend(request.pmin)(min => _.and("p.v").gte(min / 100)))
        .extend(request.pmin.isDefined && request.pmax.isDefined)(_.extend(request.pmin)(min => _.extend(request.pmax)(max => _.and("p.v").gte(min / 100).andOperator(new Criteria("p.v").lte(max / 100)))))
        .extend(request.currencyId)(currencyId => _.and("p.c").is(currencyId.id))
        .extend(request.isSale)(isSale => _.and("di").exists(true).and("di.st").lte(new Date).and("di.et").gte(new Date))
        .extend(request.name)(name => _.and("ns").in(Pattern.compile(s"^${name.toLowerCase}")))
        .and("lfs").nin(Seq(ProductLifecycleId.Removed, ProductLifecycleId.Deactivated).map(_.id).asJavaCollection)

  }

  private[this] def searchLocationProductCriteria(request: LocationProductSearchRequest, locationId: LocationId, productType: Option[ProductTypeId]) = {
    searchProductCriteria(request)
      .and("l.lid").is(locationId.id)
      .extend(productType)(pt => _.and("pt").is(pt.id))
      .extend(request.category)(category => _.and("pac.cid").in(category.id))
  }

  def searchForLocation(request: LocationProductSearchRequest, locationId: LocationId, productType: Option[ProductTypeId], offsetLimit: OffsetLimit, lang: Lang) = {
    val query = Query.query(searchLocationProductCriteria(request, locationId, productType))
     .withPaging(offsetLimit)
      .sort(request.sort.toSort(lang))
    query.fields.exclude("ns")
    mongo.find(query, entityClass).asScala.toSeq
  }

  def countSearchForLocation(request: LocationProductSearchRequest, locationId: LocationId, productType: Option[ProductTypeId]) =
    mongo.count(Query.query(searchLocationProductCriteria(request, locationId, productType)), entityClass)

  def findByAdminCriteria(params: ProductAdminSearchParams) = {
    val query = Query.query(new Criteria().filteringCriteria(params))

    val sortField = params.sort.getOrElse(ProductAdminSearchSortType.Default) match {
      case ProductAdminSearchSortType.LocationName => s"l.n.${params.lang.id}"
      case ProductAdminSearchSortType.Count => "ac"
      case ProductAdminSearchSortType.Status => "ia"
      case ProductAdminSearchSortType.Name => s"n.${params.lang.id}}"
    }

    query.asc(sortField)

    query.withPaging(params.offsetLimit)
    query.fields.exclude("ns")
    mongo.find(query, entityClass).asScala.toSeq
  }

  def countByAdminCriteria(params: ProductAdminSearchParams) = {
    val query = Query.query(new Criteria().filteringCriteria(params))
    mongo.count(query, classOf[Product])
  }

  def changeAvailability(id: ProductId, amount: Int) = {
    val q = new Query().addCriteria(productIdCriteria(id).and("ac").exists(true).gte(-amount))
    val u = new Update().inc("ac", amount)
    mongo.updateFirst(q, u, entityClass).asUpdateResult
  }

  def findByIds(productIds: Seq[ProductId]) = repo.getProducts(productIds.map(_.id)).asScala.toSeq

  def miniWishItemProductsByIds(productIds: Seq[ProductId]) = repo.getMiniWishItemProducts(productIds.map(_.id)).asScala.toSeq

  def update(changeProductParams: ChangeProductParams, lang: Lang) {

    val categoryIdToCategory = changeProductParams.additionalCategories.map(c => (c.id, c)).toMap
    val additionalCategories = changeProductParams.productCategories.map(ids => ids map categoryIdToCategory)

    val productId = changeProductParams.productId

    def extendProductWithNewKeywords(product: Product, keyWords: Unsetable[Seq[String]]): Product = keyWords match {
      case Unsetable.Update(value) => product.copy(keyWords = Some(value))
      case _ => product
    }

    def extendProductWithNewName(product: Product, keyWords: Option[String], lang: Lang): Product = keyWords match {
      case Some(value) => product.copy(name = ObjectMap(product.name.map.updated(lang.id, value)))
      case _ => product
    }

    val nameSearchUpdate: Option[Seq[String]] =
      if(changeProductParams.name.isDefined || changeProductParams.keywords != Unsetable.Skip) {
        val product = repo.findOne(productId.id)
        Some(extendProductWithNewName(extendProductWithNewKeywords(product, changeProductParams.keywords), changeProductParams.name, lang).names)
      }
      else {
        None
      }

    val query = new Query().addCriteria(productIdCriteria(productId))
    val update = (new Update).setOrSkip("pt", changeProductParams.productTypeId)
                             .setOrSkip(LocalizedField("n", lang).value, changeProductParams.name)
                             .setOrSkipSeq("ns", nameSearchUpdate)
                             .setSkipUnset("ar", changeProductParams.article)
                             .extend(changeProductParams.price)(price => _.setSerialize("p.v", price.value)
                                                              .setSerialize("p.c", price.currency))
                             .setOrSkip(LocalizedField("d", lang).value, changeProductParams.description)
                             .setOrSkip(LocalizedField("i", lang).value, changeProductParams.additionalInformation)
                             .setSkipUnsetSeq("kw", changeProductParams.keywords)
                             .extend(changeProductParams.media)(media => _.setSerializeSeq("pm",Seq(ProductMedia(MediaObject(media.imageUrl)))))
                             .setSkipUnset("lfs", changeProductParams.isActive.flatMap {
                                case true => Unset
                                case false => UnsetableUpdate(ProductLifecycleId.Deactivated)
                              })
                             .extendUnset("di", changeProductParams.discount)((discount, upd) => upd.set("di.pd", discount.percentage.value)
                             .setSkipUnset("di.st", discount.startDate)
                             .setSkipUnset("di.et", discount.endDate))
                             .setSkipUnsetSeq("pac", additionalCategories)
                             .setSkipUnset("ac", changeProductParams.count)
                             .setSkipUnset("ma", changeProductParams.count)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateAdditionalProductsCategories(locationId: LocationId, categoryId: AdditionalLocationCategoryId, newCategoryName: String, lang: Lang) {

    val updatedCategory = AdditionalLocationCategory(categoryId, ObjectMap(newCategoryName)(lang))  //todo objectMap

    val query = new Query().addCriteria(locationIdCriteria(locationId))
    val update = new Update()
                    .pull("pac", new BasicDBObject("cid", categoryId.id))

    mongo.updateFirst(query, update, entityClass)

    val update1 = new Update()
      .push("pac", AdditionalLocationCategoryConverter.serialize(updatedCategory))

    mongo.updateFirst(query, update1, entityClass)

  }

  def removeAdditionalProductsCategory(locationId: LocationId, categoryId: AdditionalLocationCategoryId) {

    val query = new Query().addCriteria(locationIdCriteria(locationId))
//    val update = new Update().pull("pac", categoryId.id)
    val update = new Update().pull("pac", new BasicDBObject("cid", categoryId.id))

    mongo.updateMulti(query, update, entityClass)

  }

  private def productIdCriteria(productId: ProductId) = new Criteria("_id").is(productId.id)

  private def locationIdCriteria(locationId: LocationId) = new Criteria("l.lid").is(locationId.id)

  def increasePresentCounter(productId: ProductId, delta: Int) = mongo.updateFirst(
    new Query().addCriteria(productIdCriteria(productId)),
    new Update().inc("c", delta),
    entityClass
  ).asUpdateResult

  def markProductAsDeleted(productId: ProductId) {
    val query = Query.query(productIdCriteria(productId))
    val update = new Update().set("lfs", ProductLifecycleId.Removed.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToS3(productId: ProductId, media: MediaObjectId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(productId.id).and("pm.u.mu").is(media.id))
    val update = new Update().set("pm.$.u.t", UrlType.s3.id).set("pm.$.u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(productId: ProductId, media: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(productId.id).and("pm.u.mu").is(media.id))
    val update = new Update().unset("pm.$.u.t")
    mongo.updateFirst(query, update, entityClass)
  }

  def getMedia(productId: ProductId) = {
    val query = Query.query(new Criteria("_id").is(productId.id)).extendProjection(Set("pm"))
    Option(mongo.findOne(query, entityClass)).map(_.productMedia.map(_.media.url))
  }

  def countProductsForLocation(id: LocationId) =
    mongo.count(Query.query(Criteria.where("l.lid").is(id.id).and("lfs").exists(false)), entityClass)
}