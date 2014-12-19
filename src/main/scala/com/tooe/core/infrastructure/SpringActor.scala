package com.tooe.core.infrastructure

import org.springframework.context.support.GenericXmlApplicationContext
import akka.actor._
import org.springframework.context.ApplicationContext
import akka.event.Logging
import akka.actor.SupervisorStrategy._
import com.tooe.extensions.scala.SettingsImpl
import com.tooe.core.boot.Started
import com.tooe.core.boot.Stop
import com.tooe.core.boot.Start
import akka.actor.OneForOneStrategy

case class LookupBean(beanClass: Class[_])
case class StartWithContext(ctxPath: String)

class SpringActor extends Actor {
  val log = Logging(context.system, this)
  var applicationContext: GenericXmlApplicationContext = _

  override val supervisorStrategy = OneForOneStrategy() {
    case _ => Resume
  }

  def receive = {
    case Start() =>
      startContext(appContext(settings.Spring.ApplicationContextPath))
      sender ! Started()

    case StartWithContext(ctxPath: String) =>
      startContext(appContext("classpath*:/META-INF/spring/"+ctxPath))
      sender ! Started()

    case Stop() =>
      applicationContext.destroy()
  }

  private lazy val settings = new SettingsImpl(context)

  private def appContext(ctxPath: String) = {
//    new GenericXmlApplicationContext(ctxPath)
    var ctx: GenericXmlApplicationContext = new GenericXmlApplicationContext();
    ctx.getEnvironment().setActiveProfiles(settings.tooeProfile);
    ctx.load(ctxPath);
    ctx.refresh();
    ctx
  }
  
  def startContext (applicationContext: GenericXmlApplicationContext) = {
    this.applicationContext = applicationContext
    val beanLookup: ActorRef = context.actorOf(
      props = Props(new BeanLookupActor(applicationContext)),
      name = "beanLookup")  
  }

}


class BeanLookupActor(applicationContext: ApplicationContext)
  extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case LookupBean(beanType) =>
//      log.info("********LookupBean for " + beanType)
      sender ! applicationContext.getBean(beanType)
  }
}

object BeanLookup {
  import akka.pattern.ask
  import scala.concurrent.Await
  import akka.util.Timeout
  import akka.actor.ActorContext

  private implicit val timeout = Timeout(10000)

  def apply[T](implicit manifest: Manifest[T], context: ActorContext) = {
    val beanLookup = context.actorFor("/user/spring/beanLookup")
    Await.result(
      (beanLookup.ask(LookupBean(manifest.erasure))(timeout)).mapTo[T](manifest),
      timeout.duration)
  }

  //  def it = this
  //  
  //  def get(beanClass: Class[_], context: ActorContext) = {
  //    val beanLookup =
  //      context.actorFor("/user/spring/beanLookup")
  //    Await.result(
  //      (beanLookup ? LookupBean(manifest.erasure)).mapTo[Class[_]](manifest),
  //      timeout.duration)
  //  }

}
