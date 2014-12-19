package com.tooe.core.util

import akka.actor.{ActorLogging, Actor}
import com.tooe.extensions.scala.{SettingsImpl, Settings}

trait ActorHelper extends ActorLogging{
  this: Actor =>

  import scala.concurrent.ExecutionContext.Implicits.global

  def settings: SettingsImpl = Settings(context.system)
}
