package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait AdminUserField extends HasIdentity with Product with Serializable

object AdminUserField extends HasIdentityFactoryEx[AdminUserField] {
  case object Name extends AdminUserField {
    def id = "name"
  }
  case object LastName extends AdminUserField {
    def id = "lastname"
  }
  case object RegDate extends AdminUserField {
    def id = "regdate"
  }
  case object Username extends AdminUserField {
    def id = "username"
  }
  case object Role extends AdminUserField {
    def id = "role"
  }
  def values = Seq(Name, LastName, RegDate, Username, Role)
}