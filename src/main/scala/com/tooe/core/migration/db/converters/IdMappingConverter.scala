package com.tooe.core.migration.db.converters

import com.tooe.core.db.mongo.converters.{DBObjectBuilder, DBObjectExtractor, DBObjectConverter, DBObjectConverters}
import com.tooe.core.migration.db.domain.{MappingCollection, IdMapping}
import org.bson.types.ObjectId
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import org.springframework.data.convert.{ReadingConverter, WritingConverter}


@WritingConverter
class IdMappingWriteConverter extends Converter[IdMapping, DBObject] with IdMappingConverter {

  def convert(source: IdMapping) = idMappingConverter.serialize(source)

}

@ReadingConverter
class IdMappingReadConverter extends Converter[DBObject, IdMapping] with IdMappingConverter {

  def convert(source: DBObject) = idMappingConverter.deserialize(source)

}


trait IdMappingConverter {

  import DBObjectConverters._

  implicit val idMappingConverter = new DBObjectConverter[IdMapping] {

    def serializeObj(obj: IdMapping) = DBObjectBuilder()
      .id.value(obj.id)
      .field("cn").value(obj.collection.toString)
      .field("oid").value(obj.ownerNewId)
      .field("lid").value(obj.legacyId)
      .field("nid").value(obj.newId)

    def deserializeObj(source: DBObjectExtractor) = IdMapping(
      id = source.id.value[ObjectId],
      collection = MappingCollection.withName(source.field("cn").value[String]),
      legacyId = source.field("lid").value[Int](0),
      newId = source.field("nid").value[ObjectId],
      ownerNewId = source.field("oid").opt[ObjectId]
    )

  }

}
