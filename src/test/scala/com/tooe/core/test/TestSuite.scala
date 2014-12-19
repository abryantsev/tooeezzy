package com.tooe.core.test

import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.specs2.mutable.Specification
import akka.testkit.ImplicitSender
import scala.concurrent.Await
import akka.actor.Props
import com.tooe.core.infrastructure.SpringActor
import akka.util.Timeout
import akka.pattern.ask
import com.tooe.core.infrastructure.StartWithContext

@RunWith(classOf[JUnitRunner])
abstract class TestSuite extends TestKit(ActorSystem()) with Specification with ImplicitSender {

  implicit val timeout = Timeout(10000)

  def getTestContext(): String

  def setUp: Any = {
    println("initspring with " + getTestContext())
    val spring = system.actorOf(
      props = Props[SpringActor],
      name = "spring")

    Await.ready(spring ? StartWithContext(getTestContext()), timeout.duration)
  }

  def tearDown: Any = {
    println("teardown after all.....")
    system.shutdown()
  }

  def logtime[T](func: => T, msg: String) : T = {
    // println(">>>> logtime...")
    val startTime = System.currentTimeMillis
    val a : T = func
    val duration = System.currentTimeMillis - startTime
    println(">>>> duration (" + msg + "): " + (duration))
    a
  }
  
}

