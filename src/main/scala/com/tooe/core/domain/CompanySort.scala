package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait CompanySort extends HasIdentity with Product with Serializable

object CompanySort extends HasIdentityFactoryEx[CompanySort] {
  case object CompanyName extends CompanySort {
    def id = "companyname"
  }
  case object ContractNumber extends CompanySort {
    def id = "contractnumber"
  }
  case object ContractDate extends CompanySort {
    def id = "contractdate"
  }
  case object ModerationStatus extends CompanySort {
    def id = "modstatus"
  }
  def values = Seq(CompanyName, ContractNumber, ContractDate, ModerationStatus)

  val Default = CompanyName
}