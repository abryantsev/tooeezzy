package com.tooe.api.marshaller

import org.specs2.matcher.JUnitMustMatchers
import org.junit.Test
import java.util.Date
import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport

class DateMarshallerTest extends JUnitMustMatchers with JacksonModuleScalaSupport {

  @Test
  def serializeDateAsSecondsSince1970InGMT {
    val d = new Date
    mapper.canSerialize(classOf[Date]) === true

    val d2 = new Date(d.getTime / 1000 * 1000)
    deserialize[Date](serialize(d)) === d2
  }
}