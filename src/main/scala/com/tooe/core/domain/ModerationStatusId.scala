package com.tooe.core.domain

case class ModerationStatusId(id: String)

object ModerationStatusId  {
  val Active = ModerationStatusId("active")
  val Waiting = ModerationStatusId("waiting")
  val Resolving = ModerationStatusId("resolving")
  val Rejected = ModerationStatusId("rejected")

  lazy val statuses = Seq(Active, Waiting, Resolving, Rejected)
}
