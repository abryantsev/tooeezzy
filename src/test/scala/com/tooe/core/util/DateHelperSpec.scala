package com.tooe.core.util

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers
import java.util.Date

class DateHelperSpec extends SpecificationWithJUnit with MustMatchers {

  "DateHelper" should {
    import DateHelper._
    "addMillis" >> {
      val d1 = currentDate
      val d2 = d1 addMillis 1
      (d2.getTime - d1.getTime) === 1
    }
    "addMinutes" >> {
      val d1 = currentDate
      val d2 = d1 addMinutes 1
      (d2.getTime - d1.getTime) === 1000*60
    }
    "addHours" >> {
      val d1 = currentDate
      val d2 = d1 addHours 1
      (d2.getTime - d1.getTime) === 1000*60*60
    }
    "addDays" >> {
      val d1 = currentDate
      val d2 = d1 addDays 1
      (d2.getTime - d1.getTime) === 1000*60*60*24
    }
    "add 30 Days bug" >> {
      val d1 = new Date(113, 11, 17, 0, 0, 0)
      val d2 = d1.addDays(30)
      d2 === new Date(114, 0, 16, 0, 0, 0)
    }
    "daysLeftTo" >> {
      val since = currentDate
      val till = since addDays 10
      (since daysLeftTo till) === 10
    }
  }
}