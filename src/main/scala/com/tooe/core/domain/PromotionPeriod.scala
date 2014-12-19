package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait PromotionPeriod extends HasIdentity

object PromotionPeriod extends HasIdentityFactoryEx[PromotionPeriod] {

  case object Day extends PromotionPeriod {
    def id = "day"
  }
  case object Week extends PromotionPeriod {
    def id = "week"
  }
  case object Month extends PromotionPeriod {
    def id = "month"
  }
  val values = Seq(Day, Week, Month)
  val Default = Day

}
