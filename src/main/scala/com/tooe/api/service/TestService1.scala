package com.tooe.api.service

import com.tooe.api.boot.TestService
import com.tooe.core.domain.UserId
import akka.actor.ActorSystem
import akka.pattern._
import spray.routing.PathMatcher
import com.tooe.core.db.graph.msg.GraphGetFriends
import akka.util.Timeout
import com.tooe.core.db.graph.msg.GraphFriends
import com.toiserver.core.usecase.notification.message._
import com.toiserver.core.usecase.registration.message.XMPPRegistration

class TestService1 (implicit val system: ActorSystem) extends SprayServiceBaseClass2 with TestService {

  import scala.concurrent.ExecutionContext.Implicits.global
  import TestService1._

  lazy val getFriendsGraphActor = lookup(Symbol("graphGetFriends"))
  lazy val notificationActor = lookup(Symbol("notificationActor"))
  lazy val xmppAccountActor = lookup(Symbol("xmppAccountActor"))

  val route = (mainPrefix & path(Root)) { routeContext: RouteContext =>
      get {
          parameter('userId )  { (userId: String) =>
            val extractedLocalValue = UserId()
            complete(
               getFriendsGraphActor.ask(new GraphGetFriends(extractedLocalValue))(Timeout(10000)).mapTo[GraphFriends]
            )
          }
      }
  } ~
    (mainPrefix & path(Email / Segment)) { (routeContext: RouteContext, emailtype: String) =>
      get {
          parameter('to)  { (to: String) =>
            val email = getEmailMsg(emailtype, to)
            println("notificationActor >> " + notificationActor)
            println("email >> " + email)
            complete(
               notificationActor.ask(email)(Timeout(10000)).mapTo[Boolean]
            )
          }
      }
    } ~ 
    (mainPrefix & path(SMS / Segment)) { (routeContext: RouteContext, emailtype: String) =>
      get {
          parameter('to)  { (to: String) =>
            val sms = getSMSMsg(emailtype, to)
            println("notificationActor >> " + notificationActor)
            println("sms >> " + sms)
            complete(
               notificationActor.ask(sms)(Timeout(10000)).mapTo[Boolean]
            )
          }
      }
     } ~ 
     (mainPrefix & path(XMPP / Segment)) { (routeContext: RouteContext, username: String) =>
      	get {
          parameter('pwd)  { (pwd: String) =>
            val xmpp = getXMPPMsg(username, pwd)
            println("xmppAccountActor >> " + xmppAccountActor)
            println("xmpp >> " + xmpp)
            complete(
               xmppAccountActor.ask(xmpp)(Timeout(10000)).mapTo[Boolean]
            )
        }
      } 
    }

    def getXMPPMsg(username: String, pwd: String): XMPPRegistration = {
      import com.toiserver.core.usecase.notification.domain._
      new XMPPRegistration(username, pwd)
    }

    def getSMSMsg(emailType: String, to: String): SMS = {
      import com.toiserver.core.usecase.notification.domain._
      new PresentSMS("resipientPhoneNumber", "resipientPhoneCountryCode", "resipientNameLastname", 
			new Sender("name", "lastName", "m"), 
			new Present("id", "name", "code", new java.util.Date()), 
			new Location("name", "country", "region", "address", "phoneNumber", "phoneCountryCode"))
    }
    
    def getEmailMsg(emailType: String, to: String): EMail = {
      import com.toiserver.core.usecase.notification.domain._
	    emailType match {
	      case "registration" => new RegistrationEmail(to, "testLink...")
	      case "newpassword" => new NewPasswordEmail(to, "newPassword...")
	      case "invitation" => new InvitationEmail(to, "resipientNameLastname...", 
	          new Sender("name", "lastName", "m"), 
	          new Location("name", "country", "region", "address", "phoneNumber", "phoneCountryCode"),
	          new java.util.Date(), "info")
	      case "present" => new PresentEmail(to, "resipientNameLastname", 
	      		new Sender("name", "lastName", "m"),
	      		new Present("id", "name", "code", new java.util.Date()), 
	      		new Location("name", "country", "region", "address", "phoneNumber", "phoneCountryCode"))
	      case "confirmation" => new ConfirmationEmail(to, "resipientNameLastname...", 
	          "presentResipientName", "presentName", 
	          new Location("name", "country", "region", "address", "phoneNumber", "phoneCountryCode"))
	    }
    }  

}



object TestService1 {
  val Root = PathMatcher("gtest1")
  val Email = PathMatcher("email")
  val SMS = PathMatcher("sms")
  val XMPP = PathMatcher("xmpp")
}
