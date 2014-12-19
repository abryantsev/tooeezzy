package com.tooe.core.domain

case class WishLifecycleId(id: String)

object WishLifecycleId {
  val Cancelled = WishLifecycleId("c")
  val Removed = WishLifecycleId("r")
  val Archived = WishLifecycleId("a")
}
