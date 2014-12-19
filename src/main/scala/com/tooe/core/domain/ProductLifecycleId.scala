package com.tooe.core.domain

case class ProductLifecycleId(id: String)

object ProductLifecycleId {
  val Deactivated = ProductLifecycleId("d")
  val Removed = ProductLifecycleId("r")
}