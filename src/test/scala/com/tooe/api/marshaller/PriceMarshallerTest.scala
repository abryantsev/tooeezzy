package com.tooe.api.marshaller

import org.specs2.matcher.JUnitMustMatchers
import org.junit.Test
import java.util.Date
import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport
import com.tooe.core.domain.{CurrencyId, Price}

class PriceMarshallerTest extends JUnitMustMatchers with JacksonModuleScalaSupport {

  @Test
  def serializePrice {
    val p = Price(value = BigDecimal(192.41), currency = CurrencyId("RUR"))
    mapper.canSerialize(classOf[Price]) === true

    serialize(p) === """{"value":192.41,"currency":"RUR"}"""

    deserialize[Price](serialize(p)) === p
  }
}