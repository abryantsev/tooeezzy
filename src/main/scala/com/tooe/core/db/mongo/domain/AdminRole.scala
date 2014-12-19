package com.tooe.core.db.mongo.domain

import com.tooe.core.domain.AdminRoleId
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "adm_user_role")
case class AdminRole(id: AdminRoleId, name: ObjectMap[String])
