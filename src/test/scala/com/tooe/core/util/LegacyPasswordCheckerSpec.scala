package com.tooe.core.util

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers

class LegacyPasswordCheckerSpec extends SpecificationWithJUnit with MustMatchers {

  "LegacyPasswordChecker" should {
    "check passwords by hash" >> {
      def check(pwd: String, hash: String) = LegacyPasswordChecker.check(password = pwd, hash = hash)
      "correct password" >> {
        check("tratata", "d27314625ba58981c9f675628bafa68f:cd") === true
      }
      "bad hash" >> {
        check("blabla", "without separator") === false
      }
      "invalid password" >> {
        check("wrongpwd", "d27314625ba58981c9f675628bafa68f:cd") === false
      }
    }
  }
}