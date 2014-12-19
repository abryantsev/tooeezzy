package com.tooe.core.boot

import scala.concurrent.Await
import com.tooe.core.application._
import com.tooe.core.infrastructure.SpringActor
import akka.actor.{ Props, ActorSystem }
import akka.pattern.ask
import akka.util.Timeout
import com.tooe.extensions.scala.Settings
import com.tooe.core.application.TitanLoadData
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.ClusterDomainEvent
import com.tooe.core.infrastructure.ClusterListenerActor
//import akka.camel.Camel
//import akka.camel.CamelExtension;

case class Start()
case class Started()
case class Stop()

trait Core {

  implicit def actorSystem: ActorSystem

  val settings = Settings(actorSystem)  
  import settings._

//  def startCamelContext = {
//    val camel = CamelExtension.get(actorSystem);
//		camel.context.setTracing(true);
//    println("tooe >> camel context started ...")
//  }
//  
//  startCamelContext
  
  def startInCluster = {
    val clusterListener = actorSystem.actorOf(
  			props = Props[ClusterListenerActor],
  			name = "clusterListener")
      
  	Cluster(actorSystem).subscribe(clusterListener, classOf[ClusterDomainEvent])  
  	//  actorSystem.actorOf(Props[MemberListenerActor], "memberListener")
    println("tooe >> cluster support started ...")
  }
  
  if(settings.tooeClustering) {
  	startInCluster    
  }

  implicit val timeout = DEFAULT_TIMEOUT

  val spring = actorSystem.actorOf(
    props = Props[SpringActor],
    name = "spring")

  def actorFactory = new AppActorFactory()

  val application = actorSystem.actorOf(
    props = Props(new ApplicationActor(actorFactory)),
    name = "application")

  Await.ready(spring ? Start(), timeout.duration)

  Await.ready(application ? Start(), timeout.duration)

  
  
  //  val infrastructure = actorSystem.actorOf(
  //    props = Props[InfrastructureActor],
  //    name = "infrastructure")
  //  Await.ready(infrastructure ? Start(), timeout.duration)
  
  //runTests()

//  private def runTests(): String = {
//	  //application ! TestSettings()
//	  //application ! TestInfrastructure()
//	  //application ! TestData()
//	  //application ! TestMysqlData()
//	  //application ! TitanLoadData()
//	  
//	  "ok"
//  }
    
}
