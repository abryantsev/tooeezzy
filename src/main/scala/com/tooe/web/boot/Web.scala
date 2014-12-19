package com.tooe.web.boot

import com.tooe.core.boot.Core
import com.tooe.api.boot.Api
//import spray.can.server.{SprayCanHttpServerApp, HttpServer}
import akka.actor._
//import spray.io.{ServerSSLEngineProvider, IOExtension, SingletonHandler}
import com.typesafe.config.ConfigFactory
import javax.net.ssl.{KeyManagerFactory, KeyManager, SSLContext}
import java.security.KeyStore
import java.io.FileInputStream
import akka.io.IO
import spray.can.Http

trait Web extends TooeSprayCanHttpServerApp{
  this: Api with Core =>

  import settings._

  if(startSprayCanHttp()){
	  println("tooe >>> starting Spray-can http server on "+ SprayCanServerHttpHost +":"+ SprayCanServerHttpPort +".............................")
//	  newHttpServer(rootService, "http-server") ! Bind(SprayCanServerHttpHost, SprayCanServerHttpPort)
    IO(Http) ! Http.Bind(rootService, interface = SprayCanServerHttpHost, port = SprayCanServerHttpPort)
  }
  
//  if(startSprayCanHttps()){
//	  implicit val mySSLContext: SSLContext = {
//	    val context = SSLContext.getInstance("TLS")
//	    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
//
//	    val ks = KeyStore.getInstance( KeyStore.getDefaultType )
//	    println("tooe >>> init KeyStore for spray-can:"+ KeyStorePath +".............................")
//	    ks.load(new FileInputStream(KeyStorePath), KeyStorePassword.toCharArray)
//	    kmf.init( ks, KeyStorePassword.toCharArray)
//
//	    context.init(kmf.getKeyManagers, null, null)
//	    context
//	  }
//
//	  implicit val myEngineProvider = ServerSSLEngineProvider { engine =>
//	    //engine.setEnabledCipherSuites(Array("TLS_RSA_WITH_AES_256_CBC_SHA"))
//	    engine.setEnabledProtocols(Array("SSLv3", "TLSv1"))
//	    engine
//	  }
//
//	  println("tooe >>> starting Spray-can https server on "+ SprayCanServerHttpsHost +":"+ SprayCanServerHttpsPort +".............................")
//	  newHttpServer(rootService, "https-server") ! Bind(SprayCanServerHttpsHost, SprayCanServerHttpsPort)
//  }
  
  // finally we drop the main thread but hook the shutdown of
  // our IOBridge into the shutdown of the applications ActorSystem
  actorSystem.registerOnTermination {
    //ioBridge.stop()
  }


}