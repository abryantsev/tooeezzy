package com.tooe.core.domain

case class OnlineStatusId(id: String)

object OnlineStatusId {
  val Busy = OnlineStatusId("busy")
  val ReadyForChat = OnlineStatusId("readyforchat")
  val Online = OnlineStatusId("online")
  val Offline = OnlineStatusId("offline")
}
