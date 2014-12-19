package com.tooe.core.main

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.kernel.Bootable
import com.tooe.api.boot.Api
import com.tooe.core.boot.Core
import com.tooe.web.boot.{ WebWithJetty, Web }
import com.tooe.extensions.scala.Settings

class StartKernel extends Bootable {
  implicit val system = SharedActorSystem.sharedMainActorSystem

  def startup() {
    val settings = Settings(system)
    import settings._

    settings.tooeProfile match {
      case "tooe-graph" => new Application(system)
      case "tooe-core" => new HttpApplication(system)
      case "tooe-migration" => new HttpApplication(system)
      case _ => new HttpApplication(system)
    }
  }

  def shutdown() { system.shutdown() }

  class Application(val actorSystem: ActorSystem) extends Core with Api with Web {
    println("tooe >> Application started ...")
  }

  class HttpApplication(val actorSystem: ActorSystem) extends Core with Api with WebWithJetty {
    println("tooe >> HttpApplication started ...")
  }
}

object SharedActorSystem {
  val sharedMainActorSystem = ActorSystem.create("tooe", defaultAkkaConfig.getConfig("akka").withFallback(defaultAkkaConfig))

  def defaultAkkaConfig = ConfigFactory.load()
}


