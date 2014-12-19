package com.tooe.core.domain

import com.tooe.core.db.mongo.domain.User
import com.tooe.api.JsonProp

case class UserInfoShort
(
  @JsonProp("id") id: UserId,
  @JsonProp("name") name: String,
  @JsonProp("lastname") lastName: Option[String],
  @JsonProp("secondname") secondName: Option[String],
  @JsonProp("media") media: MediaUrl
  )

object UserInfoShort {
  //TODO should not depend on db.mongo.domain.User
  def apply(imageSize: String)(user: User): UserInfoShort = {
    UserInfoShort(user.id, user.name, Some(user.lastName), user.secondName, user.getMainUserMediaUrl(imageSize))
  }
}