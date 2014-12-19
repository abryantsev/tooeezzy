package com.tooe.core.util

import java.util.Date
import scala.util.Try
import java.sql.Timestamp

object DateHelper {

  def currentDate = new Date()
  
  def currentTimeMillis = currentDate.getTime
  
  def currentTimeSec = currentTimeMillis / 1000

  val OneSecond: Long = 1000L
  val OneMinute = 60 * OneSecond
  val OneHour = 60 * OneMinute
  val OneDay  = 24 * OneHour
  
  implicit def dateWrapper(date: Date) = new {
    def addMillis(millis: Long) = new Date(date.getTime + millis)
    def addSeconds(seconds: Long) = addMillis(seconds * OneSecond)
    def addMinutes(minutes: Int) = addMillis(minutes * OneMinute)
    def addHours(hours: Int) = addMillis(hours * OneHour)
    def deductHours(hours: Int) = addHours(- hours)
    def addDays(days: Int) = addMillis(days * OneDay)
    def daysLeftTo(till: Date): Int = ((till.getTime - date.getTime) / OneDay).toInt
    def toTimestamp: Timestamp = new Timestamp(date.getTime)
  }

  def newTimestampIfNecessary(timestampOpt: Option[String], timestampExpirationValue: Long): Option[String] = {
    val currentTimestamp = DateHelper.currentTimeSec

    def timestampExpired(timestamp: String) =
      Try((currentTimestamp - timestamp.toLong) > timestampExpirationValue) getOrElse true

    def newTimestamp = Some(currentTimestamp.toString)

    timestampOpt match {
      case Some(ts) if timestampExpired(ts) => newTimestamp
      case None                             => newTimestamp
      case _                                => None
    }
  }

  def parseDateTime(source: String): Date = {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
    format.parse(source)
  }
}