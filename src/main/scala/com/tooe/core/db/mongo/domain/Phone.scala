package com.tooe.core.db.mongo.domain

import com.tooe.core.domain.PhoneShort

//TODO countryCode not optional
case class Phone
(
  //TODO using in User and Location, until Location doesn't have custom converter field annotations can't be omit
  @Field("c") countryCode: String = null,
  @Field("n") number: String,
  //TODO field purpose is used only for locations
  @Field("p") purpose: Option[String] = None
  ) {
  def fullNumber: String = s"${Option(countryCode).getOrElse("")} ${Option(number).getOrElse("")}".trim
  def isMain = purpose == Some("main")

  def toPhoneShort = PhoneShort(countryCode, number)
}

object Phone {
  def apply(p: PhoneShort): Phone = Phone(countryCode = p.code, number = p.number)
}