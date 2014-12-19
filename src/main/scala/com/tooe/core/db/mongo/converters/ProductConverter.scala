package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.WritingConverter
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import com.mongodb.DBObject
import com.tooe.core.domain._
import com.tooe.core.domain.LocationCategoryId
import com.tooe.core.db.mongo.domain.{ProductMedia, LocationWithName, Product}
import com.tooe.core.db.mongo.domain.{AdditionalLocationCategory, LocationWithName, Product}
import com.tooe.core.domain.Discount
import com.tooe.core.domain.ProductId

@WritingConverter
class ProductWriteConverter extends Converter[Product, DBObject] with ProductConverter {
  def convert(source: Product): DBObject = productConverter.serialize(source)
}

@ReadingConverter
class ProductReadConverter extends Converter[DBObject, Product] with ProductConverter {
  def convert(source: DBObject): Product = productConverter.deserialize(source)
}

trait ProductConverter extends PriceConverter with DiscountConverter with ProductLocationConverter with ProductMediaConverter {

  import DBObjectConverters._

  implicit val AdditionalLocationCategoryConverter = new DBObjectConverter[AdditionalLocationCategory] {
    def serializeObj(obj: AdditionalLocationCategory) = DBObjectBuilder()
      .field("cid").value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = AdditionalLocationCategory(
      id = source.field("cid").value[AdditionalLocationCategoryId],
      name = source.field("n").objectMap[String]
    )
  }

  implicit val productConverter = new DBObjectConverter[Product] {
    def serializeObj(obj: Product) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("ns").value(obj.names)
      .field("p").value(obj.price)
      .field("d").value(obj.description)
      .field("cid").value(obj.companyId)
      .field("di").value(obj.discount)
      .field("pt").value(obj.productTypeId)
      .field("pc").value(obj.productCategories)
      .field("pac").optSeq(obj.productAdditionalCategories)
      .field("c").value(obj.presentCount)
      .field("v").value(obj.validityInDays)
      .field("ac").value(obj.availabilityCount)
      .field("ma").value(obj.maxAvailabilityCount)
      .field("l").value(obj.location)
      .field("rid").value(obj.regionId)
      .field("pm").value(obj.productMedia)
      .field("ar").value(obj.article)
      .field("i").optObjectMap(obj.additionalInfo)
      .field("kw").optSeq(obj.keyWords.map(_.map(_.toLowerCase)))
      .field("lfs").value(obj.lifeCycleStatusId)

    def deserializeObj(source: DBObjectExtractor) = Product(
      id = source.id.value[ProductId],
      name = source.field("n").objectMap[String],
      companyId = source.field("cid").value[CompanyId],
      price = source.field("p").value[Price],
      description = source.field("d").objectMap[String],
      discount = source.field("di").opt[Discount],
      productTypeId = source.field("pt").value[ProductTypeId],
      productCategories = source.field("pc").seq[LocationCategoryId],
      productAdditionalCategories = source.field("pac").seqOpt[AdditionalLocationCategory],
      presentCount = source.field("c").value[Int](0),
      validityInDays = source.field("v").value[Int](0),
      availabilityCount = source.field("ac").opt[Int],
      maxAvailabilityCount = source.field("ma").opt[Int],
      location = source.field("l").value[LocationWithName],
      regionId = source.field("rid").value[RegionId],
      productMedia = source.field("pm").seq[ProductMedia],
      article = source.field("ar").opt[String],
      additionalInfo = source.field("i").optObjectMap[String],
      keyWords = source.field("kw").seqOpt[String],
      lifeCycleStatusId = source.field("lfs").opt[ProductLifecycleId]
    )
  }
}