package com.tooe.core.db.mongo.domain

import com.tooe.core.util.SomeWrapper
import com.tooe.core.domain.{MediaObjectId, MediaObject}

class UserMediaFixture {
  import SomeWrapper._
  val media = UserMedia(
    url = MediaObject(MediaObjectId("url")),
    description = "description",
    mediaType = "mediaType",
    purpose = "purpose",
    videoFormat = "videoFormat",
    descriptionStyle = "descriptionStyle",
    descriptionColor = "descriptionColor"
  )
}
