package com.tooe.core.usecase

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.core.db.mongo.domain.NewsComment
import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.infrastructure.BeanLookup
import scala.concurrent.Future
import com.tooe.core.service.NewsCommentDataService
import akka.pattern.pipe
import com.tooe.core.domain.{NewsId, NewsCommentId}
import com.tooe.api.service.OffsetLimit

object NewsCommentDataActor {
  final val Id = Actors.NewsCommentDataActor

  case class Save(entity: NewsComment)
  case class GetNewsComment(id: NewsCommentId)
  case class DeleteNewsComment(id: NewsCommentId)
  case class UpdateNewsCommentMessage(id: NewsCommentId, message: String)
  case class CountNewsCommentsByNewsId(newsId: NewsId)
  case class FindNewsCommentsByNewsId(newsId: NewsId, offsetLimit: OffsetLimit)

}

class NewsCommentDataActor extends Actor with DefaultTimeout with AppActors {

  lazy val service = BeanLookup[NewsCommentDataService]

  import scala.concurrent.ExecutionContext.Implicits.global
  import NewsCommentDataActor._

  def receive = {
    case Save(entity) => Future(service.save(entity)) pipeTo sender
    case GetNewsComment(id) => Future(service.findOne(id).getOrNotFound(id.id, "news comment")) pipeTo sender
    case DeleteNewsComment(id) => Future(service.delete(id)) pipeTo sender
    case UpdateNewsCommentMessage(id, message) => Future(service.updateMessage(id, message)) pipeTo sender
    case CountNewsCommentsByNewsId(newsId) =>  Future(service.countByNewsId(newsId)) pipeTo sender
    case FindNewsCommentsByNewsId(newsId, offsetLimit) =>  Future(service.findByNewsId(newsId, offsetLimit)) pipeTo sender
  }
}
