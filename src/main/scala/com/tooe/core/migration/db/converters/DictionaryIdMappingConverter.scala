package com.tooe.core.migration.db.converters

import com.tooe.core.db.mongo.converters.{DBObjectExtractor, DBObjectBuilder, DBObjectConverter, DBObjectConverters}
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.{MappingDictionary, DictionaryIdMapping}
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject


@WritingConverter
class DictionaryIdMappingWriteConverter extends Converter[DictionaryIdMapping, DBObject] with DictionaryIdMappingConverter {

  def convert(source: DictionaryIdMapping) = idMappingConverter.serialize(source)

}

@ReadingConverter
class DictionaryIdMappingReadConverter extends Converter[DBObject, DictionaryIdMapping] with DictionaryIdMappingConverter {

  def convert(source: DBObject) = idMappingConverter.deserialize(source)

}

trait DictionaryIdMappingConverter {

  import DBObjectConverters._

  implicit val idMappingConverter = new DBObjectConverter[DictionaryIdMapping] {

    def serializeObj(obj: DictionaryIdMapping) = DBObjectBuilder()
      .id.value(obj.id)
      .field("dn").value(obj.dictionary.toString)
      .field("lid").value(obj.legacyId)
      .field("nid").value(obj.newId)

    def deserializeObj(source: DBObjectExtractor) = DictionaryIdMapping(
      id = source.id.value[ObjectId],
      dictionary = MappingDictionary.withName(source.field("dn").value[String]),
      legacyId = source.field("lid").value[Int](0),
      newId = source.field("nid").value[String]
    )

  }

}
