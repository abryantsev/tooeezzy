package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.{Statistics, Country}
import com.tooe.core.domain.{CurrencyId, CountryId}

@WritingConverter
class CountryWriteConverter extends Converter[Country, DBObject] with CountryConverter {
  def convert(source: Country) = countryConverter.serialize(source)
}

@ReadingConverter
class CountryReadConverter extends Converter[DBObject, Country] with CountryConverter {
  def convert(source: DBObject) = countryConverter.deserialize(source)
}

trait CountryConverter extends StatisticsConverter {

  import DBObjectConverters._

  implicit val countryConverter = new DBObjectConverter[Country] {
    def serializeObj(obj: Country) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("pc").value(obj.phoneCode)
      .field("ico").value(obj.pictureFileName)
      .field("st").value(obj.statistics)
      .field("ia").value(obj.inactive)
      .field("c").value(obj.currency)

    def deserializeObj(source: DBObjectExtractor) = Country(
      id = source.id.value[CountryId],
      name = source.field("n").objectMap[String],
      phoneCode = source.field("pc").value[String],
      pictureFileName = source.field("ico").value[String],
      statistics = source.field("st").value[Statistics],
      inactive = source.field("ia").opt[Boolean],
      currency = source.field("c").opt[CurrencyId]
    )
  }
}