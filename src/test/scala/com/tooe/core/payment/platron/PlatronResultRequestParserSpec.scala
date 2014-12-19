package com.tooe.core.payment.platron

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.payment.plantron._

class PlatronResultRequestParserSpec extends SpecificationWithJUnit {

  "PlatronResultRequestParser" should {
    "parse result request" >> {
      import PlatronResultRequestParser._
      val f = new PlatronResultRequestFixture
      val request = f.request
      parse(f.asPlatronParams(request)) === request
    }
  }
}