package com.tooe.web.boot

import com.tooe.api.boot.Api
import com.tooe.core.boot.Core
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import com.typesafe.config.ConfigFactory
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import spray.servlet.{WebBoot, Servlet30ConnectorServlet, Initializer}
import org.eclipse.jetty.webapp.WebAppContext
import scala.Array
import akka.actor.{Props, ActorRef, ActorSystem}
import com.tooe.core.main.{SharedActorSystem}
import com.tooe.api.service.RoutedHttpService
import org.eclipse.jetty.server.Connector

trait WebWithJetty extends Web{
  this: Api with Core =>
    
  import settings._
  
  try {
    val server = new Server()

    var connectors = List[Connector]()
    
    if(startJettyHttp()){
	    val connector = new SelectChannelConnector()
	    connector.setHost(JettyServerHttpHost)
	    connector.setPort(JettyServerHttpPort)
	    connectors ::= connector
    }

    if(startJettyHttps()){
	    val ssl_connector = new SslSelectChannelConnector()  //HTTPSPDYServerConnector ?
	    ssl_connector.setHost(JettyServerHttpHost)
	    ssl_connector.setPort(JettyServerHttpsPort)
	    val cf = ssl_connector.getSslContextFactory
	    println("tooe >>> init KeyStore for Jetty:"+ KeyStorePath +".............................")
	    cf.setKeyStorePath(KeyStorePath)
	    cf.setTrustStore(KeyStorePath)
	    cf.setKeyStorePassword(KeyStorePassword)
	    cf.setKeyManagerPassword(KeyStorePassword)
	    connectors ::= ssl_connector
    }
    
    if(startJettyHttp() || startJettyHttps()){
    //TODO Refactoring ---> !!
    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setContextPath(JettyServerContextPath)
    // get boot class from application.conf: class SprayServletBoot extends WebBoot
    // and via listener configured with Initializer we are watching at spray servlet in Jetty
    context.addEventListener(new Initializer())
    context.addServlet(classOf[Servlet30ConnectorServlet].getName, JettyServerConnectorServletPath)

    // Configure webapp provided as external WAR/JAR
    val webapp = new WebAppContext()
    webapp.setContextPath(JettyServerContextPath)
    webapp.setWar(JettyServerWarPath)

    server.setHandler(webapp)   //TODO can't replace contexts?
    server.setHandler(context)
    // <--- Refactoring
    }
    
    if(!connectors.isEmpty){
      var ports = ""
      connectors.foreach(c => ports += c.getPort()+" ")
    	println("tooe >>> starting Jetty httpserver on "+ JettyServerHttpHost + ": "+ ports + ".............................")
    	
	    server.setConnectors(connectors.toArray)
	    server.start()
    }

  } catch {
    case e: Throwable => println("error: "+ e.toString()); e.printStackTrace()  
  }
}

class SprayServletBoot extends WebBoot {
  def system = SharedActorSystem.sharedMainActorSystem

  def serviceActor = system.actorFor("user/spray") // look-up rootService actor created inside com.tooe.api.boot.Api
}
