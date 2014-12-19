package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.LocationCategoryId
import com.tooe.core.db.mongo.domain.AdditionalLocationCategory
import com.tooe.core.db.mongo.domain.LocationMedia
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.LocationsChainId
import com.tooe.core.db.mongo.domain.LocationContact
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class PreModerationLocationWriteConverter extends Converter[PreModerationLocation, DBObject] with PreModerationLocationConverter {
  def convert(source: PreModerationLocation) = preModerationLocation.serialize(source)
}

@ReadingConverter
class PreModerationLocationReadConverter extends Converter[DBObject, PreModerationLocation] with PreModerationLocationConverter {
  def convert(source: DBObject) = preModerationLocation.deserialize(source)
}


trait PreModerationLocationConverter extends LocationContactConverter
with LocationAdditionalCategoryConverter
with LocationMediaConverter
with PreModerationStatusConverter {

  import DBObjectConverters._


  implicit val preModerationLocation = new DBObjectConverter[PreModerationLocation] {

    def serializeObj(obj: PreModerationLocation) = DBObjectBuilder()
      .id.value(obj.id)
      .field("cid").value(obj.companyId)
      .field("lcid").value(obj.locationsChainId)
      .field("n").value(obj.name)
      .field("ns").value(obj.names)
      .field("d").value(obj.description)
      .field("oh").value(obj.openingHours)
      .field("c").value(obj.contact)
      .field("lc").value(obj.locationCategories)
      .field("lac").value(obj.additionalLocationCategories)
      .field("lm").value(obj.locationMedia)
      .field("lfs").value(obj.lifecycleStatusId)
      .field("puid").value(obj.publishedLocation)
      .field("mod").value(obj.moderationStatus)

    def deserializeObj(source: DBObjectExtractor) = PreModerationLocation(
      id = source.id.value[PreModerationLocationId],
      companyId = source.field("cid").value[CompanyId],
      locationsChainId = source.field("lcid").opt[LocationsChainId],
      name = source.field("n").objectMap[String],
      description = source.field("d").objectMap[String],
      openingHours = source.field("oh").objectMap[String],
      contact = source.field("c").value[LocationContact],
      locationCategories = source.field("lc").seq[LocationCategoryId],
      additionalLocationCategories = source.field("lac").seq[AdditionalLocationCategory],
      locationMedia = source.field("lm").seq[LocationMedia],
      lifecycleStatusId = source.field("lfs").opt[LifecycleStatusId],
      moderationStatus = source.field("mod").value[PreModerationStatus],
      publishedLocation = source.field("puid").opt[LocationId]
    )
  }


}
