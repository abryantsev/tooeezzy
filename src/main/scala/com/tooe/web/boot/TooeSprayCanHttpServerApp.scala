package com.tooe.web.boot

import akka.actor.{Props, ActorRef}
//import spray.can.server.{ServerSettings, HttpServer}
//import spray.io.{IOExtension, SingletonHandler, ServerSSLEngineProvider}
import com.tooe.api.boot.Api
import com.tooe.core.boot.Core

trait TooeSprayCanHttpServerApp {
  this: Api with Core =>

//  // every spray-can HttpServer (and HttpClient) needs an IOBridge for low-level network IO
//  // (but several servers and/or clients can share one)
//  val ioBridge = IOExtension(actorSystem).ioBridge()
//
//  val Bind = HttpServer.Bind
//
//  def newHttpServer(handler: ActorRef, name: String = "http-server", settings: ServerSettings = ServerSettings())
//                   (implicit sslEngineProvider: ServerSSLEngineProvider) =
//    actorSystem.actorOf(Props(new HttpServer(ioBridge, SingletonHandler(handler), settings)), name)
}
