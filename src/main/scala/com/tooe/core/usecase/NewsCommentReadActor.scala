package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.domain.{NewsCommentId, UserId, NewsId}
import com.tooe.api.service.{ExecutionContextProvider, SuccessfulResponse, OffsetLimit}
import com.tooe.core.db.mongo.domain.NewsComment
import com.tooe.core.usecase.user.response.Author
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.api.JsonProp
import java.util.Date
import com.tooe.core.util.Images

object NewsCommentReadActor {

  val Id = Actors.NewsCommentReadActor

  case class GetAllNewsCommentsByNewsId(newsId: NewsId, offsetLimit: OffsetLimit)

}

class NewsCommentReadActor extends AppActor with ExecutionContextProvider {

  import NewsCommentReadActor._

  lazy val newsCommentDataActor = lookup(NewsCommentDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)

  def receive = {
    case GetAllNewsCommentsByNewsId(newsId, offsetLimit) =>
      (for {
        count <- countNewsCommentsByNewsId(newsId)
        comments <- findNewsCommentsByNewsId(newsId, offsetLimit)
        authors <- getAuthors(comments.map(_.authorId)).map(_.toMapId(_.id))
      } yield GetAllNewsCommentsResponse(count, comments, authors)).pipeTo(sender)
  }

  def countNewsCommentsByNewsId(newsId: NewsId) =
    newsCommentDataActor.ask(NewsCommentDataActor.CountNewsCommentsByNewsId(newsId)).mapTo[Long]

  def findNewsCommentsByNewsId(newsId: NewsId, offsetLimit: OffsetLimit) =
    newsCommentDataActor.ask(NewsCommentDataActor.FindNewsCommentsByNewsId(newsId, offsetLimit)).mapTo[Seq[NewsComment]]

  def getAuthors(userIds: Seq[UserId]) =
    userReadActor.ask(UserReadActor.SelectAuthors(userIds, Images.Newscomments.Full.Author.Media)).mapTo[Seq[Author]]

}

case class GetAllNewsCommentsResponse
(
  @JsonProp("commentscount") count: Long,
  comments: Seq[GetAllNewsCommentsResponseItem]
  ) extends SuccessfulResponse

object GetAllNewsCommentsResponse {

  def apply(count: Long, comments: Seq[NewsComment], authors: Map[UserId, Author]): GetAllNewsCommentsResponse =
    GetAllNewsCommentsResponse(count, comments.map(c => GetAllNewsCommentsResponseItem(c, authors(c.authorId))))
}

case class GetAllNewsCommentsResponseItem
(
  id: NewsCommentId,
  author: Author,
  time: Date,
  @JsonProp("msg") message: String
  ) extends UnmarshallerEntity

object GetAllNewsCommentsResponseItem {

  def apply(c: NewsComment, a: Author): GetAllNewsCommentsResponseItem =
    GetAllNewsCommentsResponseItem(c.id, a, c.creationDate, c.message)
}

