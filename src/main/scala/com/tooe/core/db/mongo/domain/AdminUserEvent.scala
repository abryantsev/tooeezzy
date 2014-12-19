package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{AdminUserId, AdminUserEventId}
import java.util.Date

@Document( collection = "adm_event" )
case class AdminUserEvent(id: AdminUserEventId = AdminUserEventId(),
                          adminUserId: AdminUserId,
                          createdTime: Date = new Date,
                          message: String)
