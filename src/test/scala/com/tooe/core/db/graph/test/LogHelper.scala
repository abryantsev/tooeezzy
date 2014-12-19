package com.tooe.core.db.graph.test

import org.specs2.matcher.MatchResult

object LogHelper {
  implicit class matchResultHelper[T](mr: => MatchResult[T]) {
    def logTimeWithMessage( msg: String) : MatchResult[T] = {
      val startTime = System.currentTimeMillis
      println(s">>>> start logging at:$startTime")
      val a : MatchResult[T] = mr
      val duration = System.currentTimeMillis - startTime
      println(">>>> duration (" + msg + "): " + duration)
      a
    }
  }
}
