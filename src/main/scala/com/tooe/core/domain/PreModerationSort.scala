package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait PreModerationSort extends HasIdentity with Product with Serializable

object PreModerationSort extends HasIdentityFactoryEx[PreModerationSort] {
  case object CompanyName extends PreModerationSort {
    def id = "companyname"
  }
  case object ContractNumber extends PreModerationSort {
    def id = "contractnumber"
  }
  case object ContractDate extends PreModerationSort {
    def id = "contractdate"
  }
  case object ModerationStatus extends PreModerationSort {
    def id = "modstatus"
  }
  def values = Seq(CompanyName, ContractNumber, ContractDate, ModerationStatus)

  val Default = CompanyName
}