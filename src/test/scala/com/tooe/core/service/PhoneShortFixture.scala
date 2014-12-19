package com.tooe.core.service

import com.tooe.core.domain.PhoneShort
import com.tooe.core.util.HashHelper

class PhoneShortFixture {
  val phoneShort = PhoneShort(
    code = HashHelper.str("code"),
    number = HashHelper.str("number")
  )
}