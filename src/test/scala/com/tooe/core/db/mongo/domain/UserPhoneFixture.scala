package com.tooe.core.db.mongo.domain

import com.tooe.core.domain._

class UserPhoneFixture {

  val phone = new PhoneFixture().phone

  val userPhone = UserPhone(
    userId = UserId(),
    phone = phone
  )

  val userMainPhone = UserPhone(
    userId = UserId(),
    phone = new PhoneFixture().randomPhone

  )
}