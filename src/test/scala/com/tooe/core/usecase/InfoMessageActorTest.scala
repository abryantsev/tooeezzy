package com.tooe.core.usecase

import com.tooe.core.util.HashHelper
import akka.testkit.TestActorRef
import akka.actor.{Props, ActorSystem}
import org.junit.Ignore

@Ignore
class InfoMessageActorTest extends ActorTestSpecification {

  import HashHelper.uuid
  import InfoMessageActor._

  def fixture = new InfoMessageTestFixture

  "InfoMessageActor" should {
    "response on GetInfoMessage" >> {
      val f = fixture
      val msg = GetInfoMessage(uuid)
      f.actor.receive(msg)
      f.actor.lastId === msg.id
    }
    "return id of the message if there is no message with such id" >> {
      val f = fixture
      val msg = GetMessage(uuid, uuid)
      f.actorRef ! msg
      expectMsg(msg.id)
      success
    }
  }

  step {
    system.shutdown()
  }
}

class InfoMessageTestFixture(implicit system: ActorSystem) {

  class InfoMessageTestActor extends InfoMessageActor {
    var lastId: String = null
    def getMessageInfo(id: String) = {
      lastId = id
      None
    }
  }
  lazy val actorRef = TestActorRef[InfoMessageTestActor](Props(new InfoMessageTestActor))
  lazy val actor = actorRef.underlyingActor
}