package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}

sealed trait PaymentPeriod extends HasIdentity

object PaymentPeriod extends HasIdentityFactoryEx[PaymentPeriod] {

  case object Day extends PaymentPeriod {
    def id = "day"
  }
  case object Month extends PaymentPeriod {
    def id = "month"
  }
  val values = Seq(Day, Month)

}
