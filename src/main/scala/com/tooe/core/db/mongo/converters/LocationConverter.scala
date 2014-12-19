package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.{WritingConverter, ReadingConverter}
import com.mongodb.DBObject
import org.bson.types.ObjectId
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._

@WritingConverter
class LocationWriteConverter extends Converter[Location, DBObject] with LocationConverter {

  def convert(source: Location): DBObject = locationConverter.serialize(source)

}

@ReadingConverter
class LocationReadConverter extends Converter[DBObject, Location] with LocationConverter {

  def convert(source: DBObject): Location = locationConverter.deserialize(source)

}

trait LocationConverter extends LocationContactConverter with LocationAdditionalCategoryConverter
with LocationMediaConverter
with LocationCountersConverter {

  import DBObjectConverters._

  implicit val locationConverter = new DBObjectConverter[Location] {

    def serializeObj(obj: Location) = DBObjectBuilder()
      .id.value(obj.id)
      .field("bid").value(obj.brandId)
      .field("cid").value(obj.companyId)
      .field("lcid").value(obj.locationsChainId)
      .field("n").value(obj.name)
      .field("ns").value(obj.names)
      .field("d").value(obj.description)
      .field("oh").value(obj.openingHours)
      .field("c").value(obj.contact)
      .field("lc").value(obj.locationCategories)
      .field("lac").value(obj.additionalLocationCategories)
      .field("pf").value(obj.hasPromotions)
      .field("lm").value(obj.locationMedia)
      .field("pa").value(obj.photoAlbums)
      .field("lp").value(obj.lastPhotos)
      .field("uf").value(obj.usersWhoFavorite)
      .field("st").value(obj.statistics)
      .field("lfs").value(obj.lifecycleStatusId)
      .field("lsr").value(obj.specialRole)

    def deserializeObj(source: DBObjectExtractor) = Location(
      id = source.id.value[LocationId],
      brandId = source.field("bid").opt[ObjectId],
      companyId = source.field("cid").value[CompanyId],
      locationsChainId = source.field("lcid").opt[LocationsChainId],
      name = source.field("n").objectMap[String],
      description = source.field("d").objectMap[String],
      openingHours = source.field("oh").objectMap[String],
      contact = source.field("c").value[LocationContact],
      locationCategories = source.field("lc").seq[LocationCategoryId],
      additionalLocationCategories = source.field("lac").seq[AdditionalLocationCategory],
      hasPromotions = source.field("pf").opt[Boolean],
      locationMedia = source.field("lm").seq[LocationMedia],
      photoAlbums = source.field("pa").seq[ObjectId],
      lastPhotos = source.field("lp").seq[LocationPhotoId],
      usersWhoFavorite = source.field("uf").seq[UserId],
      statistics = source.field("st").opt[LocationCounters].getOrElse(LocationCounters()),
      lifecycleStatusId = source.field("lfs").opt[LifecycleStatusId],
      specialRole = source.field("lsr").opt[LocationSpecialRole]
    )
  }
}