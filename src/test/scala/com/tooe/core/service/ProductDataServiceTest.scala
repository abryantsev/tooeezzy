package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import org.bson.types.ObjectId
import com.tooe.core.domain._
import com.tooe.core.util.DateHelper
import com.tooe.api.service._
import com.tooe.core.db.mongo.query._
import com.tooe.core.usecase.product.{ProductAdminSearchSortType, ProductSearchSortType}
import com.tooe.core.util.Lang
import java.util.Date
import scala.util.Random
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.usecase._
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain.Unsetable.Update

class ProductDataServiceTest extends SpringDataMongoTestHelper {
  import com.tooe.core.util.SomeWrapper._

  @Autowired var service: ProductDataService = _
  @Autowired var mongoTemplate: MongoTemplate = _


  lazy val entities = new MongoDaoHelper("product")

  def price = Price(BigDecimal(100.88), CurrencyId("RUR"))

  def locationId = LocationId(new ObjectId)
  def regionId = RegionId(new ObjectId)
  def discount = Discount(
    percent = Percent(33),
    startDate = DateHelper.currentDate,
    endDate = DateHelper.currentDate
  )

  @Test
  def saveAndRead {
    val entity = Product(
      companyId = CompanyId(),
      productTypeId = ProductTypeId.Product,
      productCategories = Seq(LocationCategoryId("some category")),
      price = price,
      discount = discount,
      location = LocationWithName(locationId, name = Map("ru" -> "locationName")),
      regionId = regionId,
      validityInDays = 30
    )
    service.findOne(entity.id.id) === None
    service.save(entity) === entity
    service.findOne(entity.id.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = Product(
      companyId = CompanyId(),
      productTypeId = ProductTypeId.Product,
      name = Map("ru" -> "name","en" -> "name2"),
      productCategories = Seq(LocationCategoryId("some category")),
      price = price,
      discount = discount,
      location = LocationWithName(locationId, name = Map("ru" -> "locationName")),
      regionId = regionId,
      availabilityCount = Option(0),
      maxAvailabilityCount = Option(0),
      additionalInfo = Option(Map("ru" -> "info")),
      keyWords = Option(Seq("keyWords")),
      lifeCycleStatusId = Option(ProductLifecycleId.Deactivated),
      validityInDays = 30
    )
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : { "$$oid" : "${entity.id.id.toString}" } ,
      "n" : { "ru" : "name", "en" : "name2" } ,
      "ns" : [ ${entity.names.mkString(",")} ] ,
      "d" : { } ,
      "cid" : { "$$oid" : "${entity.companyId.id.toString}" } ,
      "p" : { "v" : 100.88 , "c" : "RUR"} ,
      "di" : {
        "pd" : 33 ,
        "st" : ${entity.discount.get.startDate.get.mongoRepr} ,
        "et" : ${entity.discount.get.endDate.get.mongoRepr}
      } ,
      "pt" : "product" ,
      "pc" : [ "some category" ] ,
      "c" : 0 ,
      "v" : 30 ,
      "ac" : 0 ,
      "ma" : 0 ,
      "l"  : {
        "lid" : { "$$oid" : "${entity.location.id.id.toString}" } ,
        "n" : { "ru" : "locationName" }
      } ,
      "rid" : { "$$oid" : "${entity.regionId.id.toString}" } ,
      "pm" : [ ],
      "i" : { "ru" : "info" } ,
      "kw" : ["keywords"],
      "lfs": "d"
    }""")
  }

  import com.tooe.core.util.HashHelper.uuid

  @Test
  def permutationsForNameSearchField {
    val product = new ProductFixture().product.copy(name = Map(Lang.ru -> "Живописная копия картины Ренуара «Бал в Мулен де ла Галетт» "),
    keyWords = None)
    product.names.size must beLessThanOrEqualTo(5060)
  }

  @Test
  def searchAndCountByRegion {
    val entity = new ProductFixture().product.copy(
      companyId = CompanyId(),
      productTypeId = ProductTypeId.Product,
      productCategories = Seq(LocationCategoryId(uuid), LocationCategoryId(uuid)),
      productAdditionalCategories = Option(Seq(AdditionalLocationCategory(AdditionalLocationCategoryId(), ObjectMap.empty))),
      location = LocationWithName(locationId, name = Map("ru" -> "locationName")),
      regionId = regionId,
      name = Map("ru" -> "name"),
      price = price
    )
    val inactiveEntity = new ProductFixture().product.copy(lifeCycleStatusId = Some(ProductLifecycleId.Removed))
    val ctx = RouteContext("v01", Lang.ru)
    val request = ProductSearchRequest(
      category = entity.productCategories.headOption,
      name = entity.names.headOption,
      isSale = None,
      sort = Option(ProductSearchSortType.Name),
      pmax = None,
      pmin = None,
      currencyId = None,
      entities = None
    )
    def find() = service.searchByRegion(request, entity.regionId, OffsetLimit(), ctx.lang)
    def count() = service.countSearchByRegion(request, entity.regionId)
    find === Nil
    count === 0
    service.save(entity)
    service.save(inactiveEntity)
    find === Seq(entity)
    count === 1
  }

  @Test
  def searchAndCountByLocation {
    val entity = new ProductFixture().product.copy(
      companyId = CompanyId(),
      productTypeId = ProductTypeId.Product,
      productCategories = Seq(LocationCategoryId(uuid)),
      productAdditionalCategories = Option(Seq(AdditionalLocationCategory(new AdditionalLocationCategoryId(), ObjectMap.empty))),
      location = LocationWithName(locationId, name = Map("ru" -> "locationName")),
      regionId = regionId,
      name = Map("ru" -> "name"),
      price = price
    )
    val inactiveEntity = new ProductFixture().product.copy(lifeCycleStatusId = Some(ProductLifecycleId.Removed))
    val ctx = RouteContext("v01", Lang.ru)
    val request = LocationProductSearchRequest(
      category = entity.productAdditionalCategories.flatMap(_.headOption.map(_.id)),
      name = entity.names.headOption,
      isSale = None,
      sort = Option(ProductSearchSortType.Name),
      pmax = None,
      pmin = None,
      currencyId = None,
      entities = None
    )
    def find() = service.searchForLocation(request, entity.location.id, entity.productTypeId, OffsetLimit(), ctx.lang)
    def count() = service.countSearchForLocation(request, entity.location.id, entity.productTypeId)
    find === Nil
    count === 0
    service.save(entity)
    service.save(inactiveEntity)
    find === Seq(entity)
    count === 1
  }

  @Test
  def changeAvailability {
    val product = new ProductFixture().product.copy(availabilityCount = Some(1))
    service.save(product)
    service.changeAvailability(product.id, -1) === UpdateResult.Updated
    service.findOne(product.id.id).get.availabilityCount.get === 0
  }

  @Test
  def findByIds {
    val product1 = new ProductFixture().product
    val productId1, productId2 = product1.id
    service.save(product1)
    service.findByIds(Seq(productId1, productId2)) === Seq(product1)
  }

  @Test
  def miniWishItemsByIds {
    val products = (1 to 3).map(_ => service.save(new ProductFixture().product.copy(productMedia = Seq(ProductMedia(MediaObject("test.url")))))).toSeq
    val result = service.miniWishItemProductsByIds(products.map(_.id))
    result.size === products.size
    result.zip(products).foreach{
      case (f,e) =>
        f.id === e.id
        f.name === e.name
        f.productMedia === e.productMedia
    }
  }

  @Test
  def dontChangeAvailabilityIfItNotPresent {
    val product = new ProductFixture().product.copy(availabilityCount = None)
    service.save(product)
    service.changeAvailability(product.id, 3) === UpdateResult.NotFound
    service.findOne(product.id.id).get === product
  }

  @Test
  def neverChangeAvailabilityToMinus {
    val product = new ProductFixture().product.copy(availabilityCount = Some(5))
    service.save(product)
    service.changeAvailability(product.id, -6) === UpdateResult.NotFound
    service.findOne(product.id.id).get.availabilityCount.get === 5
  }

  @Test
  def update {
    val product = new ProductFixture().product.copy(lifeCycleStatusId = Option(ProductLifecycleId.Deactivated))
    service.save(product)

    implicit val lang = Lang.ru

    val additionalCategories: Seq[AdditionalLocationCategory] =
      Seq(AdditionalLocationCategory(AdditionalLocationCategoryId(), ObjectMap(Map("ru" -> "категория1"))),
          AdditionalLocationCategory(AdditionalLocationCategoryId(), ObjectMap(Map("ru" -> "категория2"))))

    val changeProductParams = ChangeProductParams(additionalCategories = additionalCategories,
                                                  productId = product.id,
                                                  name = Some("new name"),
                                                  productTypeId = ProductTypeId.Certificate,
                                                  article = Update("new article"),
                                                  price = Some(PriceChange(BigDecimal(100), CurrencyId("EUR"))),
                                                  description = Some("new description"),
                                                  additionalInformation = Some("additional information"),
                                                  keywords = Update(Seq("Keywords")),
                                                  media = Some(MediaUrl("new urls")),
                                                  isActive = Update(true),
                                                  discount = Update(DiscountChange(Percent(30),
                                                                                   Update(new Date),
                                                                                   Update(new Date))),
                                                  count = Update(15),
                                                  productCategories = Update(additionalCategories.dropRight(1).map(_.id)))

    service.update(changeProductParams, lang)

    val updatedProduct = service.findOne(product.id.id)


    updatedProduct.flatMap(_.name.localized) === changeProductParams.name
    updatedProduct.flatMap(_.productTypeId) === changeProductParams.productTypeId
    updatedProduct.flatMap(_.article) === Some(changeProductParams.article.get)
    updatedProduct.flatMap(_.price.value) === changeProductParams.price.map(_.value)
    updatedProduct.flatMap(_.price.currency) === changeProductParams.price.map(_.currency)
    updatedProduct.flatMap(_.description.localized) === changeProductParams.description
    updatedProduct.flatMap(_.additionalInfo.flatMap(_.localized)) === changeProductParams.additionalInformation
    updatedProduct.flatMap(_.keyWords) === Some(changeProductParams.keywords.get)
    updatedProduct.flatMap(_.productMedia.headOption.map(_.media.url.id)) === changeProductParams.media.map(_.imageUrl)
    updatedProduct.flatMap(_.lifeCycleStatusId) === None
    updatedProduct.flatMap(_.discount.flatMap(_.percent)) === Some(changeProductParams.discount.get.percentage)
    updatedProduct.flatMap(_.discount.flatMap(_.startDate)) === Some(changeProductParams.discount.get.startDate.get)
    updatedProduct.flatMap(_.discount.flatMap(_.endDate)) === Some(changeProductParams.discount.get.endDate.get)
    updatedProduct.flatMap(_.availabilityCount) === Some(changeProductParams.count.get)
    updatedProduct.flatMap(_.maxAvailabilityCount) === Some(changeProductParams.count.get)
    updatedProduct.flatMap(_.productAdditionalCategories) === Some(additionalCategories.dropRight(1))
//    updatedProduct.flatMap(_.productAdditionalCategories) === Some(request.productCategories.get)
}

  @Test
  def updateAdditionalProductCategory {

    val f = new ProductFixture()
    import f._

    val updatedCategory2Name = "Обновленная категория 2"
    val product = new ProductFixture().product.copy(location = location, productAdditionalCategories = productAdditionalCategories)

    service.save(product)
    service.updateAdditionalProductsCategories(f.locationId, category2.id, updatedCategory2Name, Lang.ru)

    val categories = service.findOne(product.id.id).get.productAdditionalCategories.get

    categories === Seq(category1,
                       AdditionalLocationCategory(category2.id, ObjectMap(updatedCategory2Name)(Lang.ru)))
  }

  @Test
  def removeAdditionalProductCategory {
    val f = new ProductFixture()
    import f._

    val product = new ProductFixture().product.copy(location = location, productAdditionalCategories = productAdditionalCategories)

    service.save(product)
    service.removeAdditionalProductsCategory(f.locationId, category2.id)

    service.findOne(product.id.id).get.productAdditionalCategories.get === Seq(category1)

  }

  @Test
  def searchByAdminCriteria(){
    val companyId = CompanyId()
    val locationId = LocationId()
    val name = "CoolName"
    val fixture =  new ProductFixture()
    val products = (1 to 5).map(i => fixture.productForAdminSearch(
      companyId = companyId,
      name = name.concat(i.toString),
      locationId = locationId
    ))

    val searchParams = fixture.searchParams.copy(companyId = companyId, locationId = locationId, productName = name)

    products.foreach(service.save)

    val result1 = service.findByAdminCriteria(searchParams)
    result1.length === products.length

  }

  @Test
  def countByAdminCriteria(){
    val companyId = CompanyId()
    val locationId = LocationId()
    val name = "CoolName"
    val fixture =  new ProductFixture()
    val products = (1 to 5).map(i => fixture.productForAdminSearch(
      companyId = companyId,
      name = name.concat(i.toString),
      locationId = locationId
    ))

    val searchParams = fixture.searchParams.copy(companyId = companyId, locationId = locationId, productName = name)

    products.foreach(service.save)

    val result1 = service.countByAdminCriteria(searchParams)
    result1 === products.length

  }

  @Test
  def searchByAdminCriteriaWithSorting(){
    val fixture = new ProductFixture()

    val first = fixture.productForAdminSearch(ac = 2)
    val second = fixture.productForAdminSearch(ac = 1)
    service.save(first)
    service.save(second)

    val result = service.findByAdminCriteria(fixture.searchParams.copy(sort = Option(ProductAdminSearchSortType.Count)))
    result.length === 2
    result.head === second
  }

  @Test
  def searchByAdminCriteriaWithOffset(){
    val fixture = new ProductFixture()

    val first = fixture.productForAdminSearch(ac = 2)
    val second = fixture.productForAdminSearch(ac = 1)
    service.save(first)
    service.save(second)

    val result = service.findByAdminCriteria(fixture.searchParams.copy(offsetLimit = OffsetLimit(1,1)))
    result.length === 1
    result.head === second
  }

  @Test
  def updatingNameSearchField() {
    import scala.collection.JavaConverters._

    val fixture = new ProductFixture()

    val product = fixture.product.copy(name = Map("ru" -> "CoolName1", "en" -> "CooLname2"))

    service.save(product)

    val result = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(product.id.id)), classOf[ProductNameSearch])
    Seq("coolname1","coolname2") === result.ns.asScala.toSeq

    val request = fixture.productChangeRequest.copy(name = Option("newCoolName"))

    service.update(ChangeProductParams(Nil, product.id, request), Lang.ru)

    val result2 = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(product.id.id)), classOf[ProductNameSearch])
    Seq("newcoolname","coolname2") === result2.ns.asScala.toSeq
  }

  @Test
  def increasePresentCounter {
    val product = new ProductFixture().product.copy(presentCount = 1)
    service.save(product)

    service.increasePresentCounter(product.id, 2) === UpdateResult.Updated
    service.findOne(product.id.id).get.presentCount === 3
  }

  @Test
  def markAsDeleted {
    val prod = new ProductFixture().product
    service.save(prod)
    service.markProductAsDeleted(prod.id)

    val updated = service.findOne(prod.id.id)

    updated.isEmpty === false

    updated.get === prod.copy(lifeCycleStatusId = Some(ProductLifecycleId.Removed))
    updated.get.lifeCycleStatusId.get === ProductLifecycleId.Removed
  }

  @Test
  def smallSanityTest {
    val fullProduct = Product(id = ProductId(new ObjectId),
    name = Map(Lang.ru -> "Name"),
    description = Map(Lang.ru -> "Desc"),
    companyId = CompanyId(),
    price = Price(150, CurrencyId("RUR")),
    discount = Discount(Percent(12), Some(new Date()), Some(new Date())),
    productTypeId = ProductTypeId("whatever"),
    productCategories = List(LocationCategoryId("cat1"), LocationCategoryId("cat2")),
    productAdditionalCategories = Some(Seq(AdditionalLocationCategory(AdditionalLocationCategoryId(), ObjectMap.empty),
                                       AdditionalLocationCategory(AdditionalLocationCategoryId(), ObjectMap.empty))),
    presentCount = 100500,
    validityInDays = 100500,
    availabilityCount = Some(12),
    maxAvailabilityCount= Some(13),
    location = LocationWithName(LocationId(), Map(Lang.ru -> "LocName")),
    regionId = RegionId(),
    productMedia = List(ProductMedia(MediaObject(MediaObjectId("google.com")))),
    article = Some("Article"),
    additionalInfo = Some(Map(Lang.ru -> "OptionlaInfo")),
    keyWords = Some(Seq("porn", "cats", "justin", "bieber")),
    lifeCycleStatusId = Some(ProductLifecycleId("whatever")))

    service.save(fullProduct)
    service.findOne(fullProduct.id.id).get === fullProduct
  }

  @Test
  def updateMediaStorageToS3 {
    val media1 = new MediaObjectFixture("url1", UrlType.http).mediaObject
    val media2 = new MediaObjectFixture("url2", UrlType.http).mediaObject
    val prod = new ProductFixture().product.copy(productMedia = Seq(media1, media2).map(ProductMedia(_)))
    service.save(prod)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(prod.id, media2.url, expectedMedia.url)

    service.findOne(prod.id.id).flatMap(_.productMedia.map(_.media)) === Some(Seq(media1, expectedMedia))
  }

  @Test
  def updateMediaStorageToCDN {
    val media1 = new MediaObjectFixture("url1", UrlType.s3).mediaObject
    val media2 = new MediaObjectFixture("url2",  UrlType.s3).mediaObject
    val prod = new ProductFixture().product.copy(productMedia = Seq(media1, media2).map(ProductMedia(_)))
    service.save(prod)

    service.updateMediaStorageToCDN(prod.id, media2.url)

    service.findOne(prod.id.id).flatMap(_.productMedia.map(_.media)) === Some(Seq(media1, media2.copy(mediaType = None)))
  }

  @Test
  def getMedia {
    val media = new MediaObjectFixture("url1", UrlType.s3).mediaObject
    val prod = new ProductFixture().product.copy(productMedia = Seq(media).map(ProductMedia(_)))
    service.save(prod)

    service.getMedia(prod.id) === Some(Seq(media.url))
  }

  @Test
  def countForLocation() {
    val fixture = new ProductFixture()
    val entities = (1 to 5).map(_ => service.save(fixture.product))
    service.countProductsForLocation(fixture.locationId) === entities.size

  }
}

@Document(collection = "product") case class ProductNameSearch(_id: ObjectId, ns: java.util.List[String])

class ProductFixture() {

  lazy val cId = CompanyId()
  lazy val locationId = LocationId()
  lazy val n = Option("CoolName")
  lazy val locationWithName = LocationWithName(locationId, name = Map("ru" -> "locationName"))

  def product = Product(
    id = ProductId(new ObjectId),
    companyId = CompanyId(),
    price = Price(BigDecimal(100.88), CurrencyId("RUR")),
    productTypeId = ProductTypeId.Product,
    location = locationWithName,
    regionId = RegionId(new ObjectId),
    validityInDays = 30
  )

  def productForAdminSearch(ac: Int = Random.nextInt(), companyId: CompanyId = cId, name: String = n.getOrElse(""), locationId : LocationId = locationId) =
    product.copy(availabilityCount = Option(ac), companyId = companyId, name = Map("ru" -> name), location = locationWithName.copy(id = locationId ))

  val ruLang = Lang.ru

  val searchParams = ProductAdminSearchParams(
    companyId = cId,
    locationId = Option(locationId),
    productTypeId = Option(ProductTypeId.Product),
    productName = n,
    lang = ruLang,
    offsetLimit = OffsetLimit(),
    sort = None)

  val productChangeRequest =
    ProductChangeRequest(None,None,Unsetable.Skip,None,Unsetable.Skip,None,None,Unsetable.Skip, None,Unsetable.Skip, Unsetable.Skip,Unsetable.Skip)

  val location = LocationWithName(locationId, name = ObjectMap("locationName")(ruLang))
  val category1 = AdditionalLocationCategory(AdditionalLocationCategoryId(), ObjectMap("Категория 1")(ruLang))
  val category2 = AdditionalLocationCategory(AdditionalLocationCategoryId(), ObjectMap("Категория 2")(ruLang))

  val productAdditionalCategories = Some(Seq(category1,
                                             category2))
}