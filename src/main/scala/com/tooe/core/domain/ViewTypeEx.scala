package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{UnmarshallerEntity, HasIdentity, HasIdentityFactory}

sealed trait ViewTypeEx extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object ViewTypeEx extends HasIdentityFactory[ViewTypeEx] {

  object Short extends ViewTypeEx {
    def id = "short"
  }

  object Mini extends ViewTypeEx {
    def id = "mini"
  }

  object None extends ViewTypeEx {
    def id = "none"
  }
  val values = Seq(Short, Mini, None)
  private val idToVal = values.map(v => v.id -> v).toMap

  def get(id: String) = idToVal.get(id)
}
