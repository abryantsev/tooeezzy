package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentity, HasIdentityFactoryEx}

sealed trait PresentStatus extends HasIdentity

object PresentStatus extends HasIdentityFactoryEx[PresentStatus] {
  object Valid extends PresentStatus {
    def id = "valid"
  }
  object Received extends PresentStatus {
    def id = "received"
  }
  object Expired extends PresentStatus {
    def id = "expired"
  }

  val values = Seq(Valid, Received, Expired)

}
