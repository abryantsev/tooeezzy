package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait AdminUserSort extends HasIdentity with Product with Serializable

object AdminUserSort extends HasIdentityFactoryEx[AdminUserSort] {
  case object Name extends AdminUserSort {
    def id = "name"
  }
  case object LastName extends AdminUserSort {
    def id = "lastname"
  }
  case object RegDate extends AdminUserSort {
    def id = "regdate"
  }
  case object Role extends AdminUserSort {
    def id = "role"
  }
  def values = Seq(Name, LastName, RegDate, Role)

  val Default = Name
}