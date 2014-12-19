package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.{AdminUserId, ModerationStatusId}
import java.util.Date
import com.tooe.core.db.mongo.domain.PreModerationStatus

trait PreModerationStatusConverter {

  import DBObjectConverters._

  implicit val preModerationStatusConverter = new DBObjectConverter[PreModerationStatus] {
    def serializeObj(obj: PreModerationStatus) = DBObjectBuilder()
      .field("s").value(obj.status)
      .field("m").value(obj.message)
      .field("uid").value(obj.adminUser)
      .field("t").value(obj.time)

    def deserializeObj(source: DBObjectExtractor) = PreModerationStatus(
      status = source.field("s").value[ModerationStatusId],
      message = source.field("m").opt[String],
      adminUser = source.field("uid").opt[AdminUserId],
      time = source.field("t").opt[Date]
    )
  }

}
