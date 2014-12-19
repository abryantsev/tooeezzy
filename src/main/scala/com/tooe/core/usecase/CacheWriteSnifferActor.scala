package com.tooe.core.usecase

import com.tooe.core.domain.UserId
import com.tooe.core.util.{Lang, InfoMessageHelper}
import com.tooe.core.application.Actors
import com.tooe.core.usecase.cache_writesniffer.CacheWriteSnifferDataActor
import akka.pattern.ask
import com.tooe.core.db.mongo.domain.CacheWriteSniffer
import akka.pattern.pipe
import scala.concurrent.Future
import com.tooe.core.exceptions.ApplicationException
import com.tooe.api.service.DigitalSign

object CacheWriteSnifferActor {
  final val Id = Actors.CacheWriteSniffer

  case class IsWriteActionAllowed(userId: UserId, dsignHeader: DigitalSign, lang: Lang)
}

class CacheWriteSnifferActor extends AppActor {

  import CacheWriteSnifferActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val cacheWriteSnifferDataActor = lookup(CacheWriteSnifferDataActor.Id)

  def receive = {
    case IsWriteActionAllowed(userId, dsign, lang) =>
      implicit val language = lang
      dsign.signature.map(password => checkDigitalSign(password))
        .getOrElse(
          cacheWriteSnifferDataActor.ask(CacheWriteSnifferDataActor.GetRecordsCount(userId)).mapTo[Int]
            .flatMap(recordsCount => {
              if(recordsCount >= settings.Security.WriteRequestsPerMinute) InfoMessageHelper.throwAppExceptionById("too_many_requests_in_a_given_amount_of_time")
              else cacheWriteSnifferDataActor.ask(CacheWriteSnifferDataActor.NewRecord(userId)).mapTo[CacheWriteSniffer].map(_ => true)
            })
        ) pipeTo sender

  }

  def checkDigitalSign(password: String): Future[Boolean] = {
    if (settings.Security.TooeDSignPassword == password) Future { true} else throw ApplicationException(0, "Wrong security header value")
  }
}
