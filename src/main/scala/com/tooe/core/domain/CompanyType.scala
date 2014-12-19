package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait CompanyType extends HasIdentity

object CompanyType extends HasIdentityFactoryEx[CompanyType] {
  case object Company extends CompanyType {
    def id = "company"
  }
  case object Freelancer extends CompanyType {
    def id = "freelancer"
  }
  def values = Seq(Company, Freelancer)
}
