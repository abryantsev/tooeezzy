package com.tooe.core.usecase

import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.application.AppActors
import com.tooe.core.util.{HttpClientHelper, ActorHelper}
import akka.pattern.{AskSupport, PipeToSupport}

trait AppActor extends Actor with DefaultTimeout with AppActors with ActorHelper with PipeToSupport with AskSupport with HttpClientHelper