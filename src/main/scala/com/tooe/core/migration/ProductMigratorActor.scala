package com.tooe.core.migration

import com.tooe.core.db.mongo.util.UnmarshallerEntity
import java.util.Date
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain._
import com.tooe.core.util.Lang
import com.tooe.core.domain._
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.usecase.UpdateStatisticActor
import com.tooe.core.domain.Price
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.LocationCategoryId
import com.tooe.core.db.mongo.domain.LocationWithName
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.Discount
import scala.Some
import com.tooe.core.domain.RegionId
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.domain.AdditionalLocationCategoryId
import com.tooe.core.db.mongo.domain.Product
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.migration.db.domain.IdMapping
import com.tooe.core.domain.CurrencyId
import com.tooe.core.domain.ProductId
import com.tooe.core.usecase.statistics.{ProductCategoriesUpdate, ArrayOperation, UpdateRegionOrCountryStatistic}

object ProductMigratorActor {
  val Id = 'productMigratorActor

  case class LegacyPrice(value: Int, currency: Int)

  case class LegacyProductMedia(url: String)

  case class LegacyDiscount(percentage: Int, startdate: Option[Date], enddate: Option[Date])

  case class LegacyProduct(legacyid: Int, companyid: Int, `type`: String, name: String, article: Option[String],
                           price: LegacyPrice, discount: Option[LegacyDiscount], description: String,
                           addinfo: Option[String], keywords: Option[Seq[String]], media: Option[LegacyProductMedia],
                           locationid: Int, validity: Int, availability: Option[Int],
                           maxavailability: Option[Int], categories: Option[Seq[Int]],
                           addcategories: Option[Seq[Int]], isactive: Option[Boolean],
                           admcomment: Option[String]) extends UnmarshallerEntity

}

class ProductMigratorActor extends MigrationActor {

  import ProductMigratorActor._

  def receive = {
    case lp: LegacyProduct =>
      val future = for {
        ci <- findCurrencyId(lp)
        (loc, rid) <- findLocation(lp)
        lci <- findCategories(lp, loc.id)
        alci <- findAddCategories(lp, loc.id)
        comi <- lookupByLegacyId(lp.companyid, MappingCollection.company).map(CompanyId)
        location <- (locationDataActor ? LocationDataActor.GetLocation(loc.id)).mapTo[Location]
        product <- (productDataActor ? ProductDataActor.SaveProduct(createProduct(lp, comi, ci, lci, alci, location, rid))).mapTo[Product]
        _ <- idMappingDataActor ? IdMappingDataActor.SaveIdMapping(IdMapping(new ObjectId(), MappingCollection.product, lp.legacyid, product.id.id))
      } yield {
        lp.media.foreach{
          m => saveUrl(EntityType.product, product.id.id, m.url, UrlField.ProductMain)
        }
        updateStatisticsActor ! UpdateStatisticActor.ChangeLocationProductsCounter(loc.id, 1)
        MigrationResponse(DefaultMigrationResult(lp.legacyid, product.id.id, "product_migrator"))
      }
      future pipeTo sender
  }

  def findLocation(lp: LegacyProduct): Future[(LocationWithName, RegionId)] =
    lookupByLegacyId(lp.locationid, MappingCollection.location).flatMap(id =>
      (locationDataActor ? LocationDataActor.GetLocation(LocationId(id))).mapTo[Location].map {
        loc => (LocationWithName(LocationId(id), loc.name), loc.contact.address.regionId)
      })

  def findCategories(lp: LegacyProduct, lid: LocationId): Future[Seq[LocationCategoryId]] =
    Future.traverse(lp.categories getOrElse Nil) {
      cat =>
        (dictMappingActor ? DictionaryIdMappingActor.GetLocationCategory(cat)).mapTo[LocationCategoryId]
    }

  def findAddCategories(lp: LegacyProduct, lid: LocationId): Future[Seq[AdditionalLocationCategoryId]] =
    Future.traverse(lp.addcategories getOrElse Nil)(acat =>
      lookupByLegacyId(acat, MappingCollection.locationAddCategories, Some(lid.id)).map(id =>
        AdditionalLocationCategoryId(id)))

  def findCurrencyId(lp: LegacyProduct): Future[CurrencyId] =
    (dictMappingActor ? DictionaryIdMappingActor.GetCurrency(lp.price.currency)).mapTo[CurrencyId]

  def createProduct(lp: LegacyProduct, companyId: CompanyId, currencyId: CurrencyId, prodCat: Seq[LocationCategoryId],
                    additionalCategoriesIds: Seq[AdditionalLocationCategoryId], location: Location,
                    rid: RegionId): Product = {
    val locationWithName = LocationWithName(location.id, location.name)
    val categoryIdToCategory = location.additionalLocationCategories.map(c => (c.id, c)).toMap
    val additionalCategories = additionalCategoriesIds map categoryIdToCategory

    Product(id = ProductId(new ObjectId()),
      name = Map(Lang.ru -> lp.name),
      description = Map(Lang.ru -> lp.description),
      companyId = companyId,
      price = Price(BigDecimal(lp.price.value), currencyId),
      discount = lp.discount.map(ld => Discount(ld.percentage, ld.startdate, ld.enddate)),
      productTypeId = ProductTypeId(lp.`type`),
      productCategories = prodCat,
      productAdditionalCategories = Some(additionalCategories),
      presentCount = 0,
      validityInDays = lp.validity,
      availabilityCount = lp.availability,
      maxAvailabilityCount = lp.maxavailability,
      location = locationWithName,
      regionId = rid,
      productMedia = lp.media.map(lm => ProductMedia(MediaObject(MediaObjectId(lm.url), UrlType.MigrationType))).toSeq,
      article = lp.article,
      additionalInfo = lp.addinfo.map(ai => ObjectMap.empty.copy(Map(Lang.ru.id -> ai))),
      keyWords = lp.keywords,
      lifeCycleStatusId = lp.isactive.flatMap(active => if (active) None else Option(ProductLifecycleId.Deactivated))
    )
  }

  lazy val updateStatisticsActor = lookup(UpdateStatisticActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val dictMappingActor = lookup(DictionaryIdMappingActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
}