package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{UserSendEmailEvent, UserMessageSetting}

trait UserMessageSettingsConverter extends UserSendEmailEventConverter {
  import DBObjectConverters._

  implicit val UserMessageSettingsConverter = new DBObjectConverter[UserMessageSetting] {
    def serializeObj(obj: UserMessageSetting) = DBObjectBuilder()
      .field("st").value(obj.showMessageText)
      .field("a").value(obj.soundsEnabled)
      .field("se").value(obj.sendEmailEvent)

    def deserializeObj(source: DBObjectExtractor) = UserMessageSetting(
      showMessageText = source.field("st").opt[Boolean],
      soundsEnabled = source.field("a").opt[Boolean],
      sendEmailEvent = source.field("se").opt[UserSendEmailEvent]
    )
  }
}