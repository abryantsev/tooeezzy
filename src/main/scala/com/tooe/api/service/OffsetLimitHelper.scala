package com.tooe.api.service

import spray.routing.Directives

case class OffsetLimit(offsetOpt: Option[Int], limitOpt: Option[Int], defaultOffset: Int = 0, defaultLimit: Int = 15) {
  def offset: Int = offsetOpt getOrElse defaultOffset
  def limit: Int = limitOpt getOrElse defaultLimit
}

object OffsetLimit {
  def apply(): OffsetLimit =
    OffsetLimit(offsetOpt = Some(0), limitOpt = Some(Int.MaxValue))

  def apply(offset: Int, limit: Int): OffsetLimit =
    OffsetLimit(offsetOpt = Some(offset), limitOpt = Some(limit))
}

trait OffsetLimitHelper { self: Directives =>
  val offsetLimit = getOffsetLimit(0, 15)

  def getOffsetLimit(defaultOffset:Int = 0, defaultLimit: Int = 20) =
    parameters('offset.as[Int] ?, 'limit.as[Int] ?) as ((offset: Option[Int], limit: Option[Int]) => OffsetLimit(offset, limit, defaultOffset, defaultLimit))

}