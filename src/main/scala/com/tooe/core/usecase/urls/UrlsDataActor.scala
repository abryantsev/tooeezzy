package com.tooe.core.usecase.urls

import akka.actor.Actor
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.core.db.mongo.domain.Urls
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.UrlsDataService
import scala.concurrent.Future
import akka.pattern._
import com.tooe.core.domain.{MediaObjectId, UrlType, UrlsId}
import java.util.Date
import org.bson.types.ObjectId

object UrlsDataActor {
  final val Id = Actors.UrlsData

  case class SaveUrls(urls: Urls)
  case class DeleteUrls(urlsId: UrlsId)
  case class GetLastUrls(size: Int, urlType: UrlType)
  case class SetUrlsReadTime(ids: Seq[UrlsId], date: Date)
  case class DeleteUrlByEntityAndUrl(url: (ObjectId, MediaObjectId))
  case class DeleteUrlsByEntityAndUrl(urls: Seq[(ObjectId, MediaObjectId)])

}

class UrlsDataActor extends Actor with AppActors {

  import UrlsDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[UrlsDataService]

  def receive = {

    case SaveUrls(urls) => Future { service.save(urls) } pipeTo sender
    case GetLastUrls(size, urlType) => Future { service.getLastUrls(size, urlType) } pipeTo sender
    case DeleteUrls(urlsId) => Future { service.delete(urlsId) }
    case SetUrlsReadTime(ids, date) => Future { service.setReadTime(ids, date) }
    case DeleteUrlsByEntityAndUrl(urls) => Future { service.delete(urls) }
    case DeleteUrlByEntityAndUrl(url) => self ! DeleteUrlsByEntityAndUrl(Seq(url))

  }

}
