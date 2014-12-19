package com.tooe.core.domain

case class EventGroupId(id: String)

object EventGroupId {

  val all = EventGroupId("all")
  val personal = EventGroupId("personal")
  val friend = EventGroupId("friend")
  val present = EventGroupId("present")
  val invite = EventGroupId("invite")
  val calendar = EventGroupId("calendar")
  val subscription = EventGroupId("subscription")
  val comment = EventGroupId("comment")

}