package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{LifecycleStatusId, AdminRoleId, CompanyId, AdminUserId}
import java.util.Date

@Document(collection = "adm_user")
case class AdminUser(id: AdminUserId = AdminUserId(),
                     name: String,
                     lastName: String,
                     registrationDate: Date = new Date,
                     role: AdminRoleId,
                     companyId: Option[CompanyId] = None,
                     description: Option[String] = None,
                     lifecycleStatus: Option[LifecycleStatusId] = None) {
  lazy val names = Seq(name, lastName).map(_.toLowerCase.trim).permutations.map(_.mkString(" ")).toSeq

}