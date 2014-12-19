package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{AdminUserId, AdminCredentialsId}

@Document( collection = "adm_credentials" )
case class AdminCredentials(id: AdminCredentialsId = AdminCredentialsId(),
                            adminUserId: AdminUserId,
                            userName: String,
                            password: String,
                            legacyPassword: Option[String] = None)
