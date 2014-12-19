package xxx.tooe.experiment.infrastructure

import scala.concurrent.duration.DurationInt
import com.tooe.core.boot._
import akka.actor.Actor
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy.Restart
import akka.actor.actorRef2Scala
import akka.util.Timeout
import akka.actor.ActorRef
import akka.event.Logging
import com.tooe.core.infrastructure.SpringActor
 
class InfrastructureActor extends Actor {
  val log = Logging(context.system, this)
  implicit val timeout = Timeout(30000)
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
      case _ => Restart
//		case _: NumberFormatException => Resume
//		case _: NullPointerException => Restart
//		case _: IllegalArgumentException => Stop
//		case _: Exception => Escalate
    }

  val springActor = context.actorOf(
    props = Props[SpringActor],
    name = "spring")
        
  def receive = {
    case Start() =>
      if(springActor.isInstanceOf[ActorRef]) {
	      springActor ! Start()
	      sender ! Started()
      }
    case Stop() =>
      log.info("???????????????????????????????????????????????? stop InfrastructureActor")
      springActor ! Stop()
  }
}