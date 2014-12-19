package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.Currency
import com.tooe.core.domain.CurrencyId
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import org.springframework.data.convert.{ReadingConverter, WritingConverter}

@WritingConverter
class CurrencyWriteConverter extends Converter[Currency, DBObject] with CurrencyConverter {

  def convert(source: Currency) = currencyConverter.serialize(source)

}

@ReadingConverter
class CurrencyReadConverter extends Converter[DBObject, Currency] with CurrencyConverter {

  def convert(source: DBObject) = currencyConverter.deserialize(source)

}

trait CurrencyConverter {

  import DBObjectConverters._

  implicit val currencyConverter = new DBObjectConverter[Currency] {

    def serializeObj(obj: Currency) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("c").value(obj.curs)
      .field("nc").value(obj.numcode)

    def deserializeObj(source: DBObjectExtractor) = Currency(
      id = source.id.value[CurrencyId],
      name = source.field("n").objectMap[String],
      curs = source.field("c").value[BigDecimal],
      numcode = source.field("nc").value[Int](0)
    )

  }

}