package com.tooe.core.usecase

import akka.pattern.pipe
import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.core.service.NewsLikeDataService
import com.tooe.core.infrastructure.BeanLookup
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.NewsLike
import com.tooe.core.domain.{UserId, NewsId}
import com.tooe.api.service.OffsetLimit

object NewsLikeDataActor {
  final val Id = Actors.NewsLikeDataActor

  case class Save(entity: NewsLike)
  case class Remove(newsId: NewsId, userId: UserId)
  case class FindAllByNewsId(newsId: NewsId, offsetLimit: OffsetLimit)
  case class CountByNewsId(newsId: NewsId)

}

class NewsLikeDataActor extends Actor with DefaultTimeout with AppActors {

  lazy val service = BeanLookup[NewsLikeDataService]

  import scala.concurrent.ExecutionContext.Implicits.global
  import NewsLikeDataActor._

  def receive = {
    case Save(entity) => Future(service.save(entity)) pipeTo sender
    case Remove(newsId, userId) => Future(service.remove(newsId, userId))
    case FindAllByNewsId(newsId, offsetLimit) => Future(service.findAllByNewsId(newsId, offsetLimit)) pipeTo sender
    case CountByNewsId(newsId) =>  Future(service.countByNewsId(newsId)) pipeTo sender
  }
}


