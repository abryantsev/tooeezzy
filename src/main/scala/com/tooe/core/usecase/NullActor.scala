package com.tooe.core.usecase

import akka.actor.{ActorLogging, Actor}

class NullActor extends Actor with ActorLogging {
  def receive = {
    case m => log.debug(s"NullActor got message: $m")
  }
}