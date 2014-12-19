package com.tooe.core.usecase

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.core.db.mongo.domain.NewsCommentShort
import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.NewsDataService
import scala.concurrent.Future
import com.tooe.core.domain.NewsId
import com.tooe.core.domain._
import akka.pattern.pipe
import com.tooe.core.exceptions.NotFoundException
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.domain.News
import com.tooe.core.domain.UserId
import com.tooe.core.domain.NewsId
import com.tooe.core.domain.PhotoId
import com.tooe.core.usecase.news.NewsType

object NewsDataActor {

  final val Id = Actors.NewsData

  case class Save(entity: News)
  case class FindNewsById(newsId: NewsId)
  case class UpdateUserCommentMessage(id: NewsId, message: String)
  case class DeleteUserComment(userCommentId: NewsId)
  case class UpdatePhotoAlbumPhotosAndPhotosCounter(id: NewsId, photoId: PhotoId)
  case class GetNews(newsId: NewsId)
  case class GetAllNews(userId: UserId, newsType: NewsType, offsetLimit: OffsetLimit)
  case class GetLastNewsByTypeForActor(userId: UserId, newsType: NewsTypeId)
  case class GetAllNewsForUser(userId: UserId, offsetLimit: OffsetLimit)
  case class AddNewsComment(newsId: NewsId, newsCommentShort: NewsCommentShort)
  case class DeleteNewsComment(newsId: NewsId, newsCommentId: NewsCommentId)
  case class UpdateNewsCommentMessage(newsId: NewsId, newsCommentId: NewsCommentId, message: String)
  case class LikeNews(newsId: NewsId, userId: UserId)
  case class UnlikeNews(newsId: NewsId, userId: UserId)
  case class HideNews(newsId: NewsId, userId: UserId, viewerType: NewsViewerType)
  case class RestoreNews(newsId: NewsId, userId: UserId, viewerType: NewsViewerType)
}

class NewsDataActor extends Actor with DefaultTimeout with AppActors {

  lazy val service = BeanLookup[NewsDataService]

  import scala.concurrent.ExecutionContext.Implicits.global
  import NewsDataActor._

  def receive = {
    case Save(entity) => Future(service.save(entity)) pipeTo sender
    case FindNewsById(id) => Future(service.find(id)) pipeTo sender
    case UpdateUserCommentMessage(id, message) => Future(service.updateUserCommentMessage(id, message)) pipeTo sender
    case DeleteUserComment(userCommentId) => Future(service.deleteUserComment(userCommentId))
    case UpdatePhotoAlbumPhotosAndPhotosCounter(id, photoId) => Future(service.updatePhotoAlbumPhotosAndPhotosCounter(id, photoId)) pipeTo sender
    case GetNews(newsId) => Future {
      service.find(newsId) getOrElse (throw NotFoundException(newsId.id.toString + " not found"))
    } pipeTo sender
    case GetAllNews(userId, newsType, offsetLimit) => Future {
      service.findAllNews(userId, newsType, offsetLimit)
    } pipeTo sender
    case GetAllNewsForUser(userId, offsetLimit) => Future {
      service.findAllNewsForUser(userId, offsetLimit)
    } pipeTo sender
    case GetLastNewsByTypeForActor(userId, newsType) => Future(service.findLastNewsByTypeForActor(userId, newsType)).pipeTo(sender)
    case LikeNews(id, userId) => Future(service.updateUserLikes(id, userId)).pipeTo(sender)
    case UnlikeNews(id, userId) => Future(service.updateUserUnlikes(id, userId)).pipeTo(sender)
    case AddNewsComment(id, comment) => Future(service.updateAddComment(id, comment)).pipeTo(sender)
    case DeleteNewsComment(id, commentId) => Future(service.updateDeleteComment(id, commentId)).pipeTo(sender)
    case UpdateNewsCommentMessage(id, commentId, message) => Future(service.updateNewsCommentMessage(id, commentId, message)).pipeTo(sender)
    case HideNews(id, user, viewerType) => Future(service.updateUserHideNews(id, user, viewerType)).pipeTo(sender)
    case RestoreNews(id, user, viewerType) => Future(service.updateUserRestoreNews(id, user, viewerType)).pipeTo(sender)
  }
}
