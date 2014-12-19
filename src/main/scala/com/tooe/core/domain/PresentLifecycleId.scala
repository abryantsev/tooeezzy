package com.tooe.core.domain

case class PresentLifecycleId(id: String)

object PresentLifecycleId {
  val Cancelled = PresentLifecycleId("c")
  val Removed = PresentLifecycleId("r")
  val Archived = PresentLifecycleId("a")
}