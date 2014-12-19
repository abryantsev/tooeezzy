package com.tooe.core.domain

case class NewsTypeId(id: String)

object NewsTypeId {
  val Friend = NewsTypeId("friend")
  val Checkin = NewsTypeId("checkin")
  val Message = NewsTypeId("message")
  val Present = NewsTypeId("present")
  val Photo = NewsTypeId("photo")
  val PhotoAlbum = NewsTypeId("photoalbum")
  val Wish = NewsTypeId("wish")
  val FavoriteLocation = NewsTypeId("favoritelocation")
  val Subscription = NewsTypeId("subscription")
  val EventVisitor = NewsTypeId("eventvisitor")
  val LocationNews = NewsTypeId("locationnews")
  val LocationTooeezzyNews = NewsTypeId("tooeezzynews")

  implicit def newsTypeId2EventTypeId(event: NewsTypeId) = EventTypeId(event.id)
  implicit def eventTypeId2NewsTypeId(event: EventTypeId) = NewsTypeId(event.id)

}