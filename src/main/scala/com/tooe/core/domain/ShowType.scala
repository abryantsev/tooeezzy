package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}

sealed trait ShowType extends HasIdentity with UnmarshallerEntity

object ShowType extends HasIdentityFactoryEx[ShowType] {

  object Short extends ShowType {
    def id = "short"
  }

  object None extends ShowType {
    def id = "none"
  }

  object Mini extends ShowType {
    def id = "mini"
  }

  object Adm extends ShowType {
    def id = "adm"
  }

  val values = List(Short, None, Mini, Adm)

}