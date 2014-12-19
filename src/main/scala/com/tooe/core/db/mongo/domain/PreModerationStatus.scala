package com.tooe.core.db.mongo.domain

import java.util.Date
import com.tooe.core.domain.{AdminUserId, ModerationStatusId}

case class PreModerationStatus(status: ModerationStatusId = ModerationStatusId.Waiting,
                            message: Option[String] = None,
                            adminUser: Option[AdminUserId] = None,
                            time: Option[Date] = None)
