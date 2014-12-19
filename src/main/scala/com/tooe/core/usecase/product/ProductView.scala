package com.tooe.core.usecase.product

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait ProductView extends HasIdentity {
  def id: String
}

object ProductView extends HasIdentityFactoryEx[ProductView] {

  object Full extends ProductView {
    def id = "none"
  }
  object Admin extends ProductView {
    def id = "adm"
  }

  def values = Seq(Admin, Full)

  lazy val Default = Full

}
