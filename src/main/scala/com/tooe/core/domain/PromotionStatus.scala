package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait PromotionStatus extends HasIdentity

object PromotionStatus extends HasIdentityFactoryEx[PromotionStatus] {
  case object Confirmed extends PromotionStatus {
    def id = "confirmed"
  }
  case object Rejected extends PromotionStatus {
    def id = "rejected"
  }
  def values = Confirmed :: Rejected :: Nil
}