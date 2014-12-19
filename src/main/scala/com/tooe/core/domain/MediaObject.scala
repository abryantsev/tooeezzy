package com.tooe.core.domain

case class MediaObject(url: MediaObjectId, mediaType: Option[UrlType] = Some(UrlType.s3))

object MediaObject {

  def apply(url: String, mediaType: Option[UrlType]): MediaObject = MediaObject(MediaObjectId(url), mediaType)
  def apply(url: String): MediaObject = MediaObject(MediaObjectId(url))

}
