package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait Gender extends HasIdentity

object Gender extends HasIdentityFactoryEx[Gender] {
  case object Male extends Gender {
    def id = "m"
  }
  case object Female extends Gender {
    def id = "f"
  }

  val values = Seq(Male, Female)
}