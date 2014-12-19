package com.tooe.core.domain

import com.tooe.core.db.mongo.domain.Phone

case class LocationPhone(code: String, number: String)

object LocationPhone {
  def apply(phone: Phone): LocationPhone = LocationPhone(phone.countryCode, phone.number)
}
