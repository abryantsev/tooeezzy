package com.tooe.core.db.mongo.domain

import com.tooe.core.util.{HashHelper, SomeWrapper}

class PhoneFixture {
  import SomeWrapper._
  val phone = Phone(
    countryCode = "7",
    number = "9999999999",
    purpose = "phone-purpose"
  )
  val functionalPhone = Phone(
    countryCode = "7",
    number = "1111111111",
    purpose = None
  )
  val mainFunctionalPhone = Phone(
    countryCode = "7",
    number = "5555555555",
    purpose = None
  )

  val randomPhone = Phone(
    countryCode = HashHelper.str("countryCode"),
    number = HashHelper.str("phoneNumber"),
    purpose = None
  )

}
