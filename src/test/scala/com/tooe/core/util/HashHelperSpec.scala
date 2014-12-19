package com.tooe.core.util

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers

class HashHelperSpec extends SpecificationWithJUnit with MustMatchers {

  val sample = "Some latin немного кирилицы 一些字符"
  
  "HashHelper" should {
    "calculate MD5" >> {
      HashHelper.md5(sample) === "92842d66321555c555e46ccac65fb1d3"
    }
    "calculate SHA1" >> {
      HashHelper.sha1(sample) === "eac10b5e12c1bf4aadc9bec47873c63b32d3913a"
    }
  }
}