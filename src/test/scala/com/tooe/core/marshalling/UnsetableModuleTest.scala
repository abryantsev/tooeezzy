package com.tooe.core.marshalling

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.db.mongo.util.{UnmarshallerEntity, JacksonModuleScalaSupport}
import com.tooe.core.domain.Unsetable

class UnsetableModuleTest extends SpecificationWithJUnit {

  val service = new JacksonModuleScalaSupport {}

  import UnsetableModuleTest._

  "UnsetableModule" should {
    "deserialize omitted field as Skip value" >> {
      service.deserialize[SimpleObject]("{}") === SimpleObject(Unsetable.Skip)
    }
    "deserialize an empty string field to Unset value" >> {
      service.deserialize[SimpleObject]("""{ "u" : "" }""") === SimpleObject(Unsetable.Unset)
    }

    "deserialize simple type" >> {
      val value = 199
      service.deserialize[SimpleObject](s"""{ "u" : $value  }""") === SimpleObject(Unsetable.Update(value))
    }
    "deserialize inner object" >> {
      val innerObject = InnerObject("whatever")
      service.deserialize[TestCompoundObject](s"""{ "u" : { "f" : "${innerObject.f}" } }""") ===
        TestCompoundObject(Unsetable.Update(innerObject))
    }
    "deserialize inner seq" >> {
      val innerObject = InnerObject("whatever")
      service.deserialize[TestSeqObject](s"""{ "s" : [ { "f" : "${innerObject.f}" } ] }""") ===
        TestSeqObject(Unsetable.Update(Seq(innerObject)))
    }
  }
}

object UnsetableModuleTest {

  case class SimpleObject(u: Unsetable[Long])

  case class InnerObject(f: String)

  case class TestCompoundObject(u: Unsetable[InnerObject]) extends UnmarshallerEntity

  case class TestSeqObject(s: Unsetable[Seq[InnerObject]]) extends UnmarshallerEntity
}