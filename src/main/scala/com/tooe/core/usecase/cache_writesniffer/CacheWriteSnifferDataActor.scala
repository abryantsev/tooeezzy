package com.tooe.core.usecase.cache_writesniffer

import akka.actor.Actor
import com.tooe.core.domain.UserId
import com.tooe.core.util.{DateHelper, ActorHelper}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CacheWriteSnifferDataService
import scala.concurrent.Future
import akka.pattern.pipe
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain.CacheWriteSniffer

object CacheWriteSnifferDataActor {
  final val Id = Actors.CacheWriteSnifferData

  case class NewRecord(userId: UserId)
  case class GetRecordsCount(userId: UserId)
}

class CacheWriteSnifferDataActor extends Actor with ActorHelper {

  lazy val service = BeanLookup[CacheWriteSnifferDataService]

  import CacheWriteSnifferDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case NewRecord(userId) => Future {
      val entity = CacheWriteSniffer(userId = userId, createdAt =  DateHelper.currentDate)
      service.save(entity)
    } pipeTo sender

    case GetRecordsCount(userId) => Future {
      service.recordsCount(userId)
    } pipeTo sender

  }
}