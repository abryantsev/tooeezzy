package com.tooe.core.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.domain.PreModerationStatus
import java.util.Date

case class ModerationStatusItem
(
  @JsonProperty("modstatus") status: ModerationStatusId,
  @JsonProperty("modmessage") message: Option[String],
  time: Option[Date] = None
)

object ModerationStatusItem {
  def apply(pms: PreModerationStatus): ModerationStatusItem =
    ModerationStatusItem(pms.status, pms.message, time = pms.time)
}