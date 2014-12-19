package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait StarSort extends HasIdentity {
  def field: String
}

object StarSort extends HasIdentityFactoryEx[StarSort] {

  case object Subscribers extends StarSort {
    def id = "subscriberscount"
    override def field = "star.suc"
  }
  case object Name extends StarSort {
    def id = "name"
    override def field = "n"
  }

  def values = Seq(Name, Subscribers)

}
