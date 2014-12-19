package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.CurrencyId


@Document(collection = "currency")
case class Currency
(
  id: CurrencyId,
  name: ObjectMap[String],
  curs: BigDecimal,
  numcode: Int)