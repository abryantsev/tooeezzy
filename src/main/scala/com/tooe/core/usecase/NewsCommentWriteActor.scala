package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import org.bson.types.ObjectId
import com.tooe.api.service.{DigitalSign, SuccessfulResponse}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.api.JsonProp
import com.tooe.core.util.{Lang, DateHelper}
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.exceptions.{ForbiddenAppException, ApplicationException}
import scala.concurrent.Future

object NewsCommentWriteActor {

  final val Id = Actors.NewsCommentWriteActor

  case class SaveComment(saveCommentRequest: SaveNewsCommentRequest, newsId: NewsId, authorId: UserId, dsign: DigitalSign, lang: Lang)

  case class DeleteComment(commentId: NewsCommentId, userId: UserId)

  case class UpdateComment(request: UpdateNewsCommentRequest, commentId: NewsCommentId, userId: UserId, dsign: DigitalSign, lang: Lang)

}

class NewsCommentWriteActor extends AppActor {

  import NewsCommentWriteActor._

  implicit val ec = scala.concurrent.ExecutionContext.global

  lazy val newsCommentDataActor = lookup(NewsCommentDataActor.Id)
  lazy val newsDataActor = lookup(NewsDataActor.Id)
  lazy val userEventWriteActor = lookup(UserEventWriteActor.Id)
  lazy val cacheWriteSnifferActor = lookup(CacheWriteSnifferActor.Id)

  def receive = {
    case SaveComment(saveCommentRequest, newsId, authorId, dsign, lang) =>
      (for {
        _ <- IsWriteActionAllowed(authorId, dsign, lang).recoverWith {
          case t: ApplicationException => Future failed t.copy(statusCode = t.errorCode)
          case t: Exception => Future failed t
        }
        news <- getNews(newsId)
        futureParentCommentOpt = saveCommentRequest.parentCommentId.map(getNewsComment)
        comment = commentBySaveRequest(saveCommentRequest, newsId, authorId)
        _ <- saveNewsComment(comment)
        updateResult <- addCommentToNews(newsId, comment)
        parentCommentOpt <- Future.sequence(futureParentCommentOpt.toTraversable).map(_.headOption)
      } yield updateResult match {
          case UpdateResult.Updated =>
            for {
              actorId <- news.actorId if actorId != authorId
              recipientId <- news.recipientId if recipientId != authorId
            } yield {
              val canCreateEvent = parentCommentOpt.isEmpty || parentCommentOpt.forall(parent => parent.authorId != actorId && parent.authorId != recipientId)
              if (canCreateEvent)
                userEventWriteActor ! UserEventWriteActor.NewNewsCommentReceived(comment, actorId)
            }
            for {
              actorId <- news.actorId
              futureParentComment <- futureParentCommentOpt
            } yield {
              futureParentComment.filter(pc => pc.authorId != authorId).foreach(pc => {
                userEventWriteActor ! UserEventWriteActor.NewReplyNewsCommentReceived(comment, pc.authorId)
              })
            }
            SaveNewsCommentResponse(comment)
          case _ => throw ApplicationException(message = "News comments has not been updated")
        }).pipeTo(sender)
    case DeleteComment(commentId, userId) =>
      getNewsComment(commentId).map(comment =>
        if (comment.authorId != userId)
          throw ForbiddenAppException(message = "This user can't delete this comment")
        else {
          deleteNewsComment(comment.id)
          deleteCommentFromNews(comment.newsId, comment.id)
          SuccessfulResponse
        }).pipeTo(sender)
    case UpdateComment(request, commentId, userId, dsign, lang) =>
      IsWriteActionAllowed(userId, dsign, lang).flatMap(_ => getNewsComment(commentId)).map(comment =>
        if (comment.authorId != userId)
          throw ForbiddenAppException(message = "This user can't update this comment")
        else {
          updateNewsCommentMessage(commentId, request.message)
          updateCommentInNews(comment.newsId, commentId, request.message)
          SuccessfulResponse
        }).pipeTo(sender)

  }

  def getNews(newsId: NewsId) =
    newsDataActor.ask(NewsDataActor.GetNews(newsId)).mapTo[News]

  def getNewsComment(newsCommentId: NewsCommentId) =
    newsCommentDataActor.ask(NewsCommentDataActor.GetNewsComment(newsCommentId)).mapTo[NewsComment]

  def saveNewsComment(c: NewsComment) =
    newsCommentDataActor.ask(NewsCommentDataActor.Save(c)).mapTo[NewsComment]

  def deleteNewsComment(id: NewsCommentId) =
    newsCommentDataActor ! NewsCommentDataActor.DeleteNewsComment(id)

  def updateNewsCommentMessage(id: NewsCommentId, message: String) =
    newsCommentDataActor.ask(NewsCommentDataActor.UpdateNewsCommentMessage(id, message)).mapTo[UpdateResult]

  def addCommentToNews(newsId: NewsId, c: NewsComment) =
    newsDataActor.ask(NewsDataActor.AddNewsComment(newsId, c))

  def deleteCommentFromNews(newsId: NewsId, commentId: NewsCommentId) =
    newsDataActor.ask(NewsDataActor.DeleteNewsComment(newsId, commentId))

  def updateCommentInNews(newsId: NewsId, commentId: NewsCommentId, message: String) =
    newsDataActor.ask(NewsDataActor.UpdateNewsCommentMessage(newsId, commentId, message)).mapTo[UpdateResult]


  def commentBySaveRequest(saveRequest: SaveNewsCommentRequest, newsId: NewsId, authorId: UserId): NewsComment =
    NewsComment(id = NewsCommentId(new ObjectId),
      parentId = saveRequest.parentCommentId,
      newsId = newsId,
      creationDate = DateHelper.currentDate,
      message = saveRequest.message,
      authorId = authorId)

  def IsWriteActionAllowed(userId: UserId, dsign: DigitalSign, lang: Lang) =
    cacheWriteSnifferActor.ask(CacheWriteSnifferActor.IsWriteActionAllowed(userId, dsign, lang))


  implicit def newsCommentToNewsCommentShort(c: NewsComment): NewsCommentShort =
    NewsCommentShort(c.id, c.parentId, c.creationDate, c.message, c.authorId)
}

case class SaveNewsCommentResponse(comment: SaveNewsCommentResponseItem) extends SuccessfulResponse

object SaveNewsCommentResponse {

  def apply(c: NewsComment): SaveNewsCommentResponse =
    SaveNewsCommentResponse(SaveNewsCommentResponseItem(c.id))
}

case class SaveNewsCommentResponseItem(id: NewsCommentId)

case class SaveNewsCommentRequest
(
  @JsonProp("msg") message: String,
  @JsonProp("parentid") parentCommentId: Option[NewsCommentId]
  ) extends UnmarshallerEntity

case class UpdateNewsCommentRequest(@JsonProp("msg") message: String) extends UnmarshallerEntity