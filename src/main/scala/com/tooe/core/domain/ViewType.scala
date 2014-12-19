package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}

sealed trait ViewType extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object ViewType extends HasIdentityFactoryEx[ViewType] {

  object Short extends ViewType {
    def id = "short"
  }

  object None extends ViewType {
    def id = "none"
  }

  val values = Seq(Short, None)
}