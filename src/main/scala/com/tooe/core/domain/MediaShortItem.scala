package com.tooe.core.domain

import com.tooe.core.db.mongo.domain.{LocationMedia, UserMedia}
import com.tooe.core.util.MediaHelper._

case class MediaShortItem(url: String, purpose: Option[String])

object MediaShortItem {
  def apply(media: UserMedia, imageSize: String): MediaShortItem = MediaShortItem(
    url = media.url.asUrl(imageSize), purpose = media.responsePurpose
  )
  def apply(media: LocationMedia, imageSize: String): MediaShortItem = MediaShortItem(
    url = media.url.asUrl(imageSize), purpose = media.purpose
  )
}