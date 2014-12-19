package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.{Statistics, LocationCategoryMedia, LocationCategory}
import com.tooe.core.domain.{LocationCategoryId, StarCategoryId}


@WritingConverter
class LocationCategoryWriteConverter extends Converter[LocationCategory, DBObject] with LocationCategoryConverter {

  def convert(source: LocationCategory) = locationCategoryConverter.serialize(source)
}

@ReadingConverter
class LocationCategoryReadConverter extends Converter[DBObject, LocationCategory] with LocationCategoryConverter {

  def convert(source: DBObject) = locationCategoryConverter.deserialize(source)

}

trait
LocationCategoryConverter extends LocationCategoryMediaConverter {

  import DBObjectConverters._

  implicit val locationCategoryConverter = new DBObjectConverter[LocationCategory] {
    def serializeObj(obj: LocationCategory) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("cm").value(obj.categoryMedia)

    def deserializeObj(source: DBObjectExtractor) = LocationCategory(
      id = source.id.value[LocationCategoryId],
      name = source.field("n").objectMap[String],
      categoryMedia = source.field("cm").seq[LocationCategoryMedia]
    )
  }
}