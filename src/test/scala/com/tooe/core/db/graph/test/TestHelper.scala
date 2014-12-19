package com.tooe.core.db.graph.test

import org.specs2.mutable.Specification
import scala.reflect.ClassTag
import org.specs2.matcher.{MatchResult, Matcher, BeEqualTo}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._

object TestHelper extends Specification {

  private[test] def mustEqualLoopChecker[CheckType: ClassTag](resultMagnet: ResultMagnet[CheckType])( checkValue: CheckType, timeout: Duration = Duration(10, SECONDS)) =
    awaitLoopChecker(resultMagnet)(checkValue, matcher = (v: CheckType) => new BeEqualTo(v), timeout)

  private def awaitLoopChecker[CheckType : ClassTag](resultMagnet: ResultMagnet[CheckType])(checkValue: CheckType, matcher: CheckType => Matcher[CheckType], timeout: Duration): MatchResult[CheckType] = {
    val startTime = System.currentTimeMillis
    println(">>>> starting wait loop at: " + startTime)
    def resultFunc = resultMagnet.result
    def checkResult(value: CheckType) = checkValue must matcher(value)
    def withinTimeout = System.currentTimeMillis - startTime < timeout.toMillis

    while (!checkResult(resultFunc).isSuccess && withinTimeout) {
      Thread.sleep(10)
    }
    checkResult(resultFunc)
  }

  private[test] def mustEqualChecker[CheckType: ClassTag](resultMagnet: ResultMagnet[CheckType])( checkValue: CheckType) =
    awaitChecker(resultMagnet)(checkValue, matcher = (v: CheckType) => new BeEqualTo(v))

  private def awaitChecker[CheckType: ClassTag](resultMagnet: ResultMagnet[CheckType])( checkValue: CheckType, matcher: CheckType => Matcher[CheckType]): MatchResult[CheckType] = {
    def checkResult(value: CheckType) = checkValue must matcher(value)
    def resultFunc = resultMagnet.result
    checkResult(resultFunc)
  }
}
