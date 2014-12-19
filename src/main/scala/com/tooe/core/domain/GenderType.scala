package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}


sealed trait GenderType extends HasIdentity with UnmarshallerEntity

object GenderType extends HasIdentityFactoryEx[GenderType]{

  object Male extends GenderType {
    def id: String = "male"
  }

  object Female extends GenderType {
    def id: String = "female"
  }

  def values: Seq[GenderType] = Seq(Male, Female)

  def apply(gender: Gender):GenderType =
    if (gender == Gender.Male)
      GenderType.Male
    else
      GenderType.Female
}