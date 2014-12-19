package com.tooe.core.domain

import com.tooe.core.db.mongo.domain.Phone

case class PhoneShort(code: String, number: String) {
  def render: String = code + number
}

object PhoneShort {
  @deprecated("PhoneShort shouldn't depend on db layer, use Phone.toPhoneShort instead")
  def apply(phone:Phone):PhoneShort = PhoneShort(phone.countryCode, phone.number)
}