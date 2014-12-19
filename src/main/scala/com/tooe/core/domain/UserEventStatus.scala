package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait UserEventStatus extends HasIdentity

object UserEventStatus extends HasIdentityFactoryEx[UserEventStatus] {
  case object Confirmed extends UserEventStatus {
    def id = "confirmed"
  }
  case object Rejected extends UserEventStatus {
    def id = "rejected"
  }
  def values = Seq(Confirmed, Rejected)
}