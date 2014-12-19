package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentity, HasIdentityFactoryEx}

sealed trait PresentType extends HasIdentity

object PresentType extends HasIdentityFactoryEx[PresentType] {
  object Product extends PresentType {
    def id = "product"
  }
  object Certificate extends PresentType {
    def id = "certificate"
  }

  val values = Seq(Product, Certificate)

}
