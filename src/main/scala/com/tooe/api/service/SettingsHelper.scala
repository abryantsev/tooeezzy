package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.extensions.scala.Settings


trait SettingsHelper { self: { val system: ActorSystem } =>
  def settings = Settings(system)
}