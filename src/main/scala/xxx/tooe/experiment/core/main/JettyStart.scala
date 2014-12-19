package xxx.tooe.experiment.core.main

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import spray.servlet.{WebBoot, Servlet30ConnectorServlet, Initializer}
import akka.actor.{ActorSystem, ActorRef}
import com.typesafe.config.ConfigFactory
import com.tooe.core.boot.Core
import com.tooe.api.boot.Api
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.webapp.WebAppContext
import com.tooe.core.main.SharedActorSystem
import spray.servlet.Servlet30ConnectorServlet

class JettyStart extends WebBoot {
  def system: ActorSystem = JettyStart.system

  def serviceActor: ActorRef = JettyStart.app.rootService
}

object JettyStart extends App{

  implicit val system = SharedActorSystem.sharedMainActorSystem

  def apply() = new JettyStart()

  class Application(val actorSystem: ActorSystem) extends Core
  with Configuration with Api {

  }

  val app = new Application(system)

  try {
    println(">>> STARTING EMBEDDED JETTY SERVER")
//    val server = new Server()
//
//    val connector = new SelectChannelConnector()
//    connector.setPort(ConfigFactory.load().getInt("jetty.server.http.port"))
//
//    val ssl_connector = new SslSelectChannelConnector()  //HTTPSPDYServerConnector ?
//    ssl_connector.setPort(ConfigFactory.load().getInt("jetty.server.https.port"))
//    //ssl_connector.setMaxIdleTime()
//    //ssl_connector.setAcceptors(100)
//    val cf = ssl_connector.getSslContextFactory
//
//    val keyStorePath = getClass.getClassLoader.getResource("jettyKeyStore").getPath
//
//    cf.setKeyStorePath(keyStorePath)
//    cf.setTrustStore(keyStorePath)    //?
//    cf.setKeyStorePassword("tooejetty")    //move constants to Config
//    cf.setKeyManagerPassword("tooejetty")
//
//    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
//    context.setContextPath("/")
//    context.addEventListener(new Initializer())
//    context.addServlet(classOf[Servlet30ConnectorServlet].getName, "/*")
//
//    // Configure webapp provided as external WAR
//    val webapp = new WebAppContext()
//    webapp.setContextPath("/")
//    webapp.setWar("target/tooe-0.0.1-SNAPSHOT.jar")
//    server.setHandler(webapp)
//
//
//    server.setHandler(context)
//    server.setConnectors(Array(connector, ssl_connector))
//    server.start()

  } catch {
    case e:Throwable => e.printStackTrace()
  }

  sys.addShutdownHook {
    system.shutdown()
  }



}