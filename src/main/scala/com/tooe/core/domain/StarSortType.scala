package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait StarSortType extends HasIdentity

object StarSortType extends HasIdentityFactoryEx[StarSortType] {
  case object SubscribersCount extends StarSortType {
    def id = "subscriberscount"
  }
  def values = Seq(SubscribersCount)
}