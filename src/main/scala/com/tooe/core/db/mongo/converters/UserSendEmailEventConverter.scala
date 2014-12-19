package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.UserSendEmailEvent

trait UserSendEmailEventConverter {
  import DBObjectConverters._

  implicit val UserSendEmailEventConverter = new DBObjectConverter[UserSendEmailEvent] {
    def serializeObj(obj: UserSendEmailEvent) = DBObjectBuilder()
      .field("e").value(obj.email)
      .field("ev").value(obj.events)

    def deserializeObj(source: DBObjectExtractor) = UserSendEmailEvent(
      email = source.field("e").value[String],
      events = source.field("ev").seq[String]
    )
  }
}