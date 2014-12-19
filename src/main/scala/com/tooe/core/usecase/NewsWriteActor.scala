package com.tooe.core.usecase

import com.tooe.api.JsonProp
import com.tooe.api.service.{Photo => _, _}
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.domain._
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.exceptions.ForbiddenAppException
import com.tooe.core.usecase.location_subscription.LocationSubscriptionDataActor
import com.tooe.core.util.{DateHelper, Lang}
import scala.Some
import scala.concurrent.Future
import user.UserDataActor

object NewsWriteActor {
  final val Id = Actors.NewsWrite

  case class LeaveComment(request: LeaveCommentRequest, userId: UserId, dsign: DigitalSign, lang: Lang)
  case class UpdateComment(request: UpdateUserCommentRequest, userCommentId: NewsId, currentUserId: UserId)
  case class RemoveComment(userCommentId: NewsId, currentUserId: UserId)
  case class AddFavoriteLocation(userId: UserId, locationId: LocationId)
  case class AddWishNews(userId: UserId, wishId: WishId, product: Product)
  case class AddCheckinNews(userId: UserId, locationId: LocationId)
  case class AddLocationSubscriptionsNews(userId: UserId, locationId: LocationId)
  case class AddLocationNews(locationNews: LocationNews, lang: Lang)
  case class AddSpecialLocationNews(locationNews: LocationNews, lang: Lang)
  case class AddFriendShipNews(user1: UserId, user2: UserId)
  case class AddPhotoNews(userId: UserId, photoAlbumId: PhotoAlbumId, photoId: PhotoId)
  case class AddPhotoAlbumNews(userId: UserId, photoAlbumId: PhotoAlbumId)
  case class AddPresentNews(present: Present)
  case class LikeNews(userId: UserId, newsId: NewsId)
  case class UnlikeNews(userId: UserId, newsId: NewsId)
  case class HideNews(newsId: NewsId, currentUserId: UserId)
  case class RestoreNews(newsId: NewsId, currentUserId: UserId)
}

class NewsWriteActor extends AppActor with ExecutionContextProvider with FriendReadComponent {

  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val newsDataActor = lookup(NewsDataActor.Id)
  lazy val newsLikeDataActor = lookup(NewsLikeDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val photoAlbumDataActor = lookup(PhotoAlbumDataActor.Id)
  lazy val locationSubscriptionDataActor = lookup(LocationSubscriptionDataActor.Id)
  lazy val userEventWriteActor = lookup(UserEventWriteActor.Id)
  lazy val cacheWriteSnifferActor = lookup(CacheWriteSnifferActor.Id)

  import NewsWriteActor._

  def receive = {
    case LikeNews(userId, newsId) =>
      for {
        news <- getNews(newsId)
        newsLike <- saveNewsLike(userId, newsId)
        updateResult <- updateNewsLike(userId, newsId)
      } yield updateResult match {
        case UpdateResult.Updated =>
          news.actorId.filterNot(_ == userId).foreach(actor => userEventWriteActor ! UserEventWriteActor.NewNewsLikeReceived(newsLike, actor))
        case _ => throw ApplicationException(0, "News like count has not been updated")
      }
      sender ! SuccessfulResponse

    case UnlikeNews(userId, newsId) =>
      for {
        updateResult <- updateNewsUnlike(userId, newsId)
      } yield updateResult match {
        case UpdateResult.Updated =>
          newsLikeDataActor ! NewsLikeDataActor.Remove(newsId, userId)
        case _ => throw ApplicationException(0, "News like count has not been updated")
      }
      sender ! SuccessfulResponse

    case UpdateComment(request, userCommentId, currentUserId) =>
      (for {
        newsOpt <- findNews(userCommentId)
        _ <- validateActionUnderComment(newsOpt, currentUserId)
        result <- updateCommentFtr(userCommentId, request)
      } yield result) pipeTo sender

    case LeaveComment(request, currentUserId, dsign, lang) =>
      cacheWriteSnifferActor.ask(CacheWriteSnifferActor.IsWriteActionAllowed(currentUserId, dsign, lang)).mapTo[Boolean].flatMap(_ => {
        for {
          user <- getUser(request.userId)
        } yield {
          val news = News(
            viewers = Seq(currentUserId, request.userId).distinct,
            newsType = NewsTypeId.Message,
            actorId = Option(currentUserId),
            recipientId = Option(request.userId),
            actorsAndRecipientsIds = Seq(currentUserId, request.userId).distinct,
            userComment = Option(NewsUserComment(request.message))
          )
          newsDataActor ! NewsDataActor.Save(news)
          if (request.userId != currentUserId) {
            userEventWriteActor ! UserEventWriteActor.NewCommentOnTheWallReceived(news)
          }
          LeaveCommentResponse(LeaveCommentResponseItem(news.id))
        }
      }) pipeTo sender

    case RemoveComment(userCommentId, currentUserId) =>
      (for {
        userCommentOpt <- findNews(userCommentId)
        _ <- validateActionUnderComment(userCommentOpt, currentUserId)
      } yield {
        removeUserComment(userCommentId)
        SuccessfulResponse
      }) pipeTo sender

    case AddPresentNews(present) =>
      present.userId.foreach(recipient =>
        getUserFriends(recipient).map {
          friends =>
            val news = News(
              viewers = (friends + recipient).filterNot(user => present.actorId.exists(_ == user)).toSeq,
              newsType = NewsTypeId.Present,
              actorId = present.actorId,
              recipientId = present.userId,
              actorsAndRecipientsIds = (present.actorId ++ present.userId).toSeq.distinct,
              present = Some(NewsPresent(present))
            )
            newsDataActor ! NewsDataActor.Save(news)
        })

    case AddFavoriteLocation(userId, locationId) =>
      getUserFriends(userId).map {
        friends =>
          val news = News(
            viewers = friends.toSeq,
            newsType = NewsTypeId.FavoriteLocation,
            actorId = Option(userId),
            actorsAndRecipientsIds = Seq(userId),
            favoriteLocation = Some(NewsFavoriteLocation(locationId))
          )
          newsDataActor ! NewsDataActor.Save(news)
      }

    case AddWishNews(userId, wishId, product) =>
      getUserFriends(userId).map {
        friends =>
          val news = News(
            viewers = friends.toSeq,
            newsType = NewsTypeId.Wish,
            actorId = Option(userId),
            actorsAndRecipientsIds = Seq(userId),
            wish = Some(NewsWish(wishId, product.location.id, product.id))
          )
          newsDataActor ! NewsDataActor.Save(news)
      }

    case AddCheckinNews(userId, locationId) =>
      getUserFriends(userId).map {
        friends =>
          val news = News(
            viewers = friends.toSeq,
            newsType = NewsTypeId.Checkin,
            actorId = Option(userId),
            actorsAndRecipientsIds = Seq(userId),
            checkin = Some(NewsCheckin(locationId))
          )
          newsDataActor ! NewsDataActor.Save(news)
      }

    case AddLocationSubscriptionsNews(userId, locationId) =>
      getUserFriends(userId).map {
        friends =>
          val news = News(
            viewers = friends.toSeq,
            newsType = NewsTypeId.Subscription,
            actorId = Option(userId),
            actorsAndRecipientsIds = Seq(userId),
            subscription = Some(NewsSubscription(locationId))
          )
          newsDataActor ! NewsDataActor.Save(news)
      }

    case AddLocationNews(locationNews, lang) =>
      implicit val l = lang
      for {
        ls <- getLocationSubscriptionsByLocation(locationNews.locationId)
        subscribers = ls.map(_.userId)
        news = News(
          viewers = subscribers,
          newsType = NewsTypeId.LocationNews,
          actorsAndRecipientsIds = Seq.empty,
          locationNews = Some(NewsOfLocation(locationNews.locationId, locationNews.content.localized.getOrElse("")))
        )
      } yield newsDataActor ! NewsDataActor.Save(news)

    case AddSpecialLocationNews(locationNews, lang) =>
      implicit val l = lang
      for {
        ls <- getLocationSubscriptionsByLocation(locationNews.locationId)
        news = News(
          viewers = Seq(),
          newsType = NewsTypeId.LocationTooeezzyNews,
          actorsAndRecipientsIds = Seq.empty,
          locationNews = Some(NewsOfLocation(locationNews.locationId, locationNews.content.localized.getOrElse("")))
        )
      } yield newsDataActor ! NewsDataActor.Save(news)

    case AddFriendShipNews(user1, user2) =>
      for {
        friends1 <- getUserFriends(user1)
        friends2 <- getUserFriends(user2)
      } yield {
        val mutualFriends = friends2 intersect friends1
        val distinctFriends2 = (friends2 diff mutualFriends) - user1
        val news1 = News(
          viewers = (friends1 - user2).toSeq,
          newsType = NewsTypeId.Friend,
          actorId = Some(user1),
          recipientId = Some(user2),
          actorsAndRecipientsIds = Seq(user1))
        val news2 = News(
          viewers = distinctFriends2.toSeq,
          newsType = NewsTypeId.Friend,
          actorId = Some(user2),
          recipientId = Some(user1),
          actorsAndRecipientsIds = Seq(user2))
        newsDataActor ! NewsDataActor.Save(news1)
        newsDataActor ! NewsDataActor.Save(news2)
      }

    case AddPhotoAlbumNews(userId, photoAlbumId) =>
      for {
        friends <- getUserFriends(userId)
        photoAlbum <- getPhotoAlbum(photoAlbumId)
        photos <- getPhotos(photoAlbumId)
        news = News(
          viewers = friends.toSeq,
          newsType = NewsTypeId.PhotoAlbum,
          actorId = Option(userId),
          actorsAndRecipientsIds = Seq(userId),
          photoAlbum = Option(NewsPhotoAlbum(photoAlbumId, 1, photos.takeRight(10).map(_.id)))
        )
      } yield newsDataActor ! NewsDataActor.Save(news)

    case AddPhotoNews(userId, photoAlbumId, photoId) =>
      def isUserContinueUploadingPhotos(news: News) =
        DateHelper.currentDate.getTime - news.createdAt.getTime < settings.PhotoUpload.AggregateUploadedPhotoTime
      getLastPhotoNews(userId).zip(getLastPhotoAlbumNews(userId)).map {
        case (Some(lastPhotoNews), _) if isUserContinueUploadingPhotos(lastPhotoNews) =>
          newsDataActor ! NewsDataActor.UpdatePhotoAlbumPhotosAndPhotosCounter(lastPhotoNews.id, photoId)

        case (_, Some(lastPhotoAlbumNews)) if isUserContinueUploadingPhotos(lastPhotoAlbumNews) =>
          newsDataActor ! NewsDataActor.UpdatePhotoAlbumPhotosAndPhotosCounter(lastPhotoAlbumNews.id, photoId)

        case _ => for {
          friends <- getUserFriends(userId)
          photoAlbum <- getPhotoAlbum(photoAlbumId)
          news = News(
            viewers = friends.toSeq,
            newsType = NewsTypeId.Photo,
            actorId = Option(userId),
            actorsAndRecipientsIds = Seq(userId),
            photoAlbum = Option(NewsPhotoAlbum(photoAlbumId, 1, Seq(photoId)))
          )
        } yield newsDataActor ! NewsDataActor.Save(news)
      }

    case HideNews(newsId, userId) =>
      val result = for {
        news <- getNews(newsId).map {
          case n if n.newsType == NewsTypeId.LocationTooeezzyNews => throw ApplicationException(message = "Invalid news type")
          case n => n
        }
        result <- news.actorsAndRecipientsIds.find(_ == userId).map(hideNews(newsId, _, NewsViewerType.ActorOrRecipient))
          .orElse(news.viewers.find(_ == userId).map(hideNews(newsId, _, NewsViewerType.Viewer))).getOrElse(Future.successful())
          .map(_ => SuccessfulResponse)
      } yield result
      result.pipeTo(sender)

    case RestoreNews(newsId, userId) =>
      getNews(newsId).map {
        news =>
          news.hidedActorsAndRecipients.find(_ == userId).map(restoreNews(newsId, _, NewsViewerType.ActorOrRecipient))
            .orElse(news.hidedUsers.find(_ == userId).map(restoreNews(newsId, _, NewsViewerType.Viewer)))
            .fold[SuccessfulResponse.type](throw ApplicationException(message = "Invalid news"))(_ => SuccessfulResponse)
      }.pipeTo(sender)

  }

  def hideNews(id: NewsId, userId: UserId, viewerType: NewsViewerType) =
    newsDataActor.ask(NewsDataActor.HideNews(id, userId, viewerType)).mapTo[UpdateResult]

  def restoreNews(id: NewsId, userId: UserId, viewerType: NewsViewerType) =
    newsDataActor.ask(NewsDataActor.RestoreNews(id, userId, viewerType)).mapTo[UpdateResult]


  def getLastPhotoNews(userId: UserId) =
    newsDataActor.ask(NewsDataActor.GetLastNewsByTypeForActor(userId, NewsTypeId.Photo)).mapTo[Option[News]]

  def getLastPhotoAlbumNews(userId: UserId) =
    newsDataActor.ask(NewsDataActor.GetLastNewsByTypeForActor(userId, NewsTypeId.PhotoAlbum)).mapTo[Option[News]]

  def validateActionUnderComment(newsOpt: Option[News], currentUserId: UserId): Future[_] = Future {
    if (newsOpt.flatMap(_.actorId).exists(_ != currentUserId))
      throw ForbiddenAppException("Action denied")
  }

  def updateCommentFtr(userCommentId: NewsId, request: UpdateUserCommentRequest): Future[SuccessfulResponse.type] = {
    newsDataActor.ask(NewsDataActor.UpdateUserCommentMessage(userCommentId, request.message)).mapTo[UpdateResult].map {
      case UpdateResult.Updated => SuccessfulResponse
      case _ => throw ApplicationException(0, "Comment has not been updated")
    }
  }

  def removeUserComment(userCommentId: NewsId): Unit = {
    newsDataActor ! NewsDataActor.DeleteUserComment(userCommentId)
  }

  def findNews(userCommentId: NewsId): Future[Option[News]] = {
    newsDataActor.ask(NewsDataActor.FindNewsById(userCommentId)).mapTo[Option[News]]
  }

  def getNews(newsId: NewsId) =
    newsDataActor.ask(NewsDataActor.GetNews(newsId)).mapTo[News]

  def getUser(userId: UserId): Future[User] = {
    userDataActor.ask(UserDataActor.GetUser(userId)).mapTo[User]
  }

  def getLocationSubscriptionsByLocation(locationId: LocationId) =
    locationSubscriptionDataActor.ask(LocationSubscriptionDataActor.FindLocationSubscriptionsByLocation(locationId)).mapTo[Seq[LocationSubscription]]

  def getPhotoAlbum(photoAlbumId: PhotoAlbumId) =
    photoAlbumDataActor.ask(PhotoAlbumDataActor.GetPhotoAlbumById(photoAlbumId)).mapTo[PhotoAlbum]

  def getPhotos(photoAlbumId: PhotoAlbumId) =
    photoDataActor.ask(PhotoDataActor.GetAllPhotosByAlbum(photoAlbumId)).mapTo[Seq[Photo]]

  def saveNewsLike(userId: UserId, newsId: NewsId) =
    newsLikeDataActor.ask(NewsLikeDataActor.Save(NewsLike(userId, newsId))).mapTo[NewsLike]

  def updateNewsLike(userId: UserId, newsId: NewsId) =
    newsDataActor.ask(NewsDataActor.LikeNews(newsId, userId)).mapTo[UpdateResult]

  def updateNewsUnlike(userId: UserId, newsId: NewsId) =
    newsDataActor.ask(NewsDataActor.UnlikeNews(newsId, userId)).mapTo[UpdateResult]


}

case class LeaveCommentRequest(@JsonProp("userid") userId: UserId,
                               @JsonProp("msg") message: String) extends UnmarshallerEntity

case class LeaveCommentResponse(@JsonProp("usercomment") userComment: LeaveCommentResponseItem) extends SuccessfulResponse

case class LeaveCommentResponseItem(id: NewsId)

case class UpdateUserCommentRequest(@JsonProp("msg") message: String) extends UnmarshallerEntity
