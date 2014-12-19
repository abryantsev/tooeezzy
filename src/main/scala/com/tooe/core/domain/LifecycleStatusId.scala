package com.tooe.core.domain

case class LifecycleStatusId(id: String)

object LifecycleStatusId {
  val Deactivated = LifecycleStatusId("d")
  val Removed = LifecycleStatusId("r")
  val Archived = LifecycleStatusId("a")
}