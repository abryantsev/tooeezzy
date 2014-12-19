package com.tooe.core.usecase

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{ActorRef, ActorSystem}
import org.specs2.mutable.Specification
import akka.testkit.DefaultTimeout
import scala.concurrent.{Await, Future, Promise}

@RunWith(classOf[JUnitRunner])
abstract class ActorTestSpecification(actorSystem: ActorSystem = ActorSystem()) extends TestKit(actorSystem)
  with Specification
  with ImplicitSender
  with DefaultTimeout
{

  implicit def actorRefTestHelper(actorRef: ActorRef) = new {
    def probeTells(msg: AnyRef, sender: TestProbe = TestProbe()): TestProbe = {
      actorRef.tell(msg, sender.ref)
      sender
    }
  }
  
  implicit def futureTestHelper[T](f: Future[T]) = new {
    def awaitResult: T = Await.result(f, timeout.duration)
  } 
  
  implicit def promiseTestHelper[T](p: Promise[T]) = new {
    def awaitResult: T = p.future.awaitResult
  } 
}