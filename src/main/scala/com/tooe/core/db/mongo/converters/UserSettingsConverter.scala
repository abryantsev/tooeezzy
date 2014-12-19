package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{UserMessageSetting, UserSettings}

trait UserSettingsConverter extends UserMessageSettingsConverter {
  import DBObjectConverters._

  implicit val UserSettingsConverter = new DBObjectConverter[UserSettings] {
    def serializeObj(obj: UserSettings) = DBObjectBuilder()
      .field("pr").value(obj.pageRights)
      .field("mr").value(obj.mapRights)
      .field("sa").value(obj.showAge)
      .field("ms").value(obj.messageSettings)

    def deserializeObj(source: DBObjectExtractor) = UserSettings(
      pageRights = source.field("pr").seq[String],
      mapRights = source.field("mr").seq[String],
      showAge = source.field("sa").value[Boolean](false),
      messageSettings = source.field("ms").value[UserMessageSetting]
    )
  }
}
