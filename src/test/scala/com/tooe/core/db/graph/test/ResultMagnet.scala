package com.tooe.core.db.graph.test

import scala.reflect.ClassTag
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._

trait ResultMagnet[Result] {
  def result: Result
}

object ResultMagnet {
  implicit def fromFuture[T : ClassTag](future: Future[T]) =
    new ResultMagnet[T] {
      def result: T = {
        Await.result(future.mapTo[T], Duration(2, SECONDS))
      }
    }

  implicit def fromCustomType[T](obj: T) =
    new ResultMagnet[T] {
      def result: T = { obj }
    }
}