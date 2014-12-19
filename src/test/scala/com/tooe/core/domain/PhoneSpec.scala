package com.tooe.core.domain

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.db.mongo.domain.Phone

class PhoneSpec extends SpecificationWithJUnit {

  "Phone" should {

    "correct format when code is null" in {
      Phone(null, "1234").fullNumber === "1234"
    }

    "correct format when number is null" in {
      Phone("+7", null).fullNumber === "+7"
    }

    "empty string when not set code and number" in {
      Phone(null, null).fullNumber === ""
    }

    "correct phone format with valid data" in {
      Phone("+7", "1234").fullNumber === "+7 1234"
    }

  }

}
