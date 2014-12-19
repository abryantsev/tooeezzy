package com.tooe.core.service

import com.tooe.core.domain.{MediaObjectId, MediaObject, UrlType}
import com.tooe.core.util.HashHelper

class MediaObjectFixture(url: String = HashHelper.str("url"), storage: UrlType = UrlType.s3) {

  val mediaObject = MediaObject(MediaObjectId(url), Some(storage))

}
