package com.tooe.core.db.mongo.domain

import org.bson.types.ObjectId
import com.tooe.core.db.mongo.util.{PermutationHelper, UnmarshallerEntity}
import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain._
import java.util.Date

@Document(collection = "product")
case class Product
(
  id: ProductId = ProductId(new ObjectId),
  name: ObjectMap[String] = ObjectMap.empty,
  description: ObjectMap[String] = ObjectMap.empty,
  companyId: CompanyId,
  price: Price,
  discount: Option[Discount] = None,
  productTypeId: ProductTypeId,
  productCategories: Seq[LocationCategoryId] = Nil,
  productAdditionalCategories: Option[Seq[AdditionalLocationCategory]] = None,
  presentCount: Int = 0,
  validityInDays: Int,
  availabilityCount: Option[Int] = None,
  maxAvailabilityCount: Option[Int] = None,
  location: LocationWithName,
  regionId: RegionId,
  productMedia: Seq[ProductMedia] = Nil,
  article: Option[String] = None,
  additionalInfo: Option[ObjectMap[String]] = None,
  keyWords: Option[Seq[String]] = None,
  lifeCycleStatusId: Option[ProductLifecycleId] = None
  ) extends UnmarshallerEntity {

  lazy val names = keyWords.toSeq.flatten ++ name.values.flatMap(PermutationHelper.permuteBySpace)

  def priceWithDiscount(date: Date) = {
    val discountPercent = discount flatMap (_.percentAt(date)) getOrElse 0
    price withDiscount discountPercent
  }
}

case class LocationWithName(id: LocationId, name: ObjectMap[String])

case class ProductMedia(media: MediaObject)
