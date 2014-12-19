  package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.StarCategory
import com.tooe.core.domain.StarCategoryId

@WritingConverter
class StarCategoryWriteConvert extends Converter[StarCategory, DBObject] with StarCategoryConverter {
  def convert(obj: StarCategory) = starsCategoryConverter.serialize(obj)
}

@ReadingConverter
class StarCategoryReadConvert extends Converter[DBObject, StarCategory] with StarCategoryConverter {
  def convert(source: DBObject) = starsCategoryConverter.deserialize(source)
}

trait StarCategoryConverter {

  import DBObjectConverters._

  implicit val starsCategoryConverter = new DBObjectConverter[StarCategory] {
    def serializeObj(obj: StarCategory) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("d").value(obj.description)
      .field("cm").value(obj.categoryMedia)
      .field("c").value(obj.starsCount)
      .field("pid").value(obj.parentId)

    def deserializeObj(source: DBObjectExtractor) = StarCategory(
        id = source.id.value[StarCategoryId],
        name = source.field("n").objectMap[String],
        description = source.field("d").objectMap[String],
        categoryMedia = source.field("cm").value[String],
        starsCount = source.field("c").value[Int](0),
        parentId = source.field("pid").opt[StarCategoryId])
  }

}
