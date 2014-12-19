package com.tooe.core.domain

import org.bson.types.ObjectId
import com.tooe.core.util.MediaHelper._

case class MediaItem(id: ObjectId, url: String)

object MediaItem {

  def apply(mo: MediaItemDto, imageSize: String): MediaItem = MediaItem(mo.id, mo.mediaObject.asUrl(imageSize))

}

case class MediaItemDto(id: ObjectId, mediaObject: MediaObject)