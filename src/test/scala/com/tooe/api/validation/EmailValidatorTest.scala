package com.tooe.api.validation

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers

class EmailValidatorTest extends SpecificationWithJUnit with MustMatchers {

  "EmailValidator" should {
    implicit val path: String = ""
    "validate email" >> {
      EmailValidator("a@b.c") === ValidationSucceed
      EmailValidator("a@c").failed === true
    }
  }
}