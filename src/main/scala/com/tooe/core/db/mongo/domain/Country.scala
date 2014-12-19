package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{CurrencyId, CountryId}

@Document( collection = "country" )
case class Country
(
  id: CountryId,
  name: ObjectMap[String] = ObjectMap.empty,
  phoneCode: String,
  pictureFileName: String,
  statistics: Statistics = Statistics(),
  inactive: Option[Boolean] = None,
  currency: Option[CurrencyId] = None
) {
  def defaultCurrency = currency.getOrElse(CurrencyId("RUR"))
}