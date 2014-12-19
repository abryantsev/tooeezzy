package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait PromotionFields extends HasIdentity

object PromotionFields extends HasIdentityFactoryEx[PromotionFields] {
  case object Promotions extends PromotionFields {
    def id = "promotions"
  }
  case object PromotionsShort extends PromotionFields {
    def id = "promotions(short)"
  }
  case object PromotionsCount extends PromotionFields {
    def id = "promotionscount"
  }
  def values = Seq(Promotions, PromotionsShort, PromotionsCount)
}
