package com.tooe.core.payment.platron

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.payment.plantron._

class PlatronCheckRequestParserSpec extends SpecificationWithJUnit {

  "PlatronCheckRequestParser" should {
    "parse check request" >> {
      import PlatronCheckRequestParser._
      val f = PlatronCheckRequestFixture
      val request = f.request
      parse(f.asPlatronParams(request)) === request
    }
  }
}