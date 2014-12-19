package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{CompanyId, SessionToken, AdminUserId, AdminRoleId}
import java.util.Date

@Document(collection = "cache_adm_sessions")
case class CacheAdminSession
(
  id: SessionToken,
  time: Date,
  adminUserId: AdminUserId,
  role: AdminRoleId,
  companies: Seq[CompanyId]
  )
{
  def companiesOpt = if (companies.isEmpty) None else Some(companies)
}
