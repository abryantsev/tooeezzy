package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait PartnershipType extends HasIdentity

object PartnershipType extends HasIdentityFactoryEx[PartnershipType] {
  case object Partner extends PartnershipType {
    def id = "partner"
  }
  case object Dealer extends PartnershipType {
    def id = "dealer"
  }
  def values = Seq(Partner, Dealer)
}
