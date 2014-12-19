package com.tooe.core.service

import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.db.mongo.domain.{NewsCommentShort, News, NewsPhotoAlbum}
import com.tooe.core.domain._
import com.tooe.core.util.{DateHelper, HashHelper}
import java.util.Date
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import scala.Some
import com.tooe.core.usecase.news.NewsType

class NewsDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: NewsDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("news")

  @Test
  def readWrite() {
    val f = new NewsFixture
    import f._
    service.find(news.id) === None
    service.save(news)
    service.find(news.id) === Some(news)
  }

  @Test
  def representation() {
    val f = new NewsFixture
    import f._
    import news._
    service.save(news)
    val repr = entities.findOne(news.id.id)
    jsonAssert(repr)( s"""{
      "_id" : ${id.id.mongoRepr},
      "aid" : ${news.actorId.get.id.mongoRepr},
      "arid" : [ ${news.actorId.get.id.mongoRepr}, ${news.recipientId.get.id.mongoRepr}],
      "hus" : [],
      "har" : [],
      "cc" : ${news.commentsCount},
      "cs" : [],
      "lc" : ${news.likesCount},
      "ls" : [],
      "nt" : ${news.newsType.id},
      "t" : ${news.createdAt.mongoRepr},
      "uids" : [],
      "rid" : ${news.recipientId.get.id.mongoRepr}
    }""")
  }

  @Test
  def updateUserCommentMessage {
    val f = new NewsFixture
    import f._
    service.save(news)
    val newMessage = HashHelper.str("message")
    service.updateUserCommentMessage(news.id, newMessage)
    service.find(news.id).flatMap(_.userComment.map(_.message)) === Some(newMessage)
  }

  @Test
  def deleteUserComment {
    val f = new NewsFixture
    import f._
    service.save(news)
    service.find(news.id) === Some(news)
    service.deleteUserComment(news.id)
    service.find(news.id) === None
  }

  @Test
  def findAllNews {
    val f = new NewsFixture
    import f._
    val currentUser = UserId()
    val newsOfCurrentUser = news.copy(id = NewsId(), viewers = news.viewers :+ currentUser, actorsAndRecipientsIds = news.actorsAndRecipientsIds :+ currentUser)
    val newsFromTooeezzy = news.copy(id = NewsId(), viewers = Seq.empty, newsType = NewsTypeId.LocationTooeezzyNews, actorsAndRecipientsIds = Seq.empty)
    service.save(news)
    service.save(newsOfCurrentUser)
    service.save(newsFromTooeezzy)
    service.findAllNews(currentUser, NewsType.My ,OffsetLimit(0, 2)).zip(Seq(newsOfCurrentUser, newsFromTooeezzy)).foreach {case (f, e) => f === e }
    service.findAllNews(currentUser, NewsType.All, OffsetLimit(0, 3)).map(_.id) must containAllOf( Seq(newsOfCurrentUser, newsFromTooeezzy).map(_.id))
    service.delete(newsFromTooeezzy.id)
  }

  @Test
  def updateUserLikes() {
    val f = new NewsFixture
    import f._
    service.save(news)
    service.updateUserLikes(news.id, actorId)
    val result = service.find(news.id)
    result.map(_.usersWhoLike) === Some(Seq(actorId))
    result.map(_.likesCount) === Some(1)
  }

  @Test
  def updateUserUnlikes() {
    val f = new NewsFixture
    import f._

    val userId1, userId2 = UserId()

    service.save(news)
    service.updateUserLikes(news.id, userId1)
    service.updateUserLikes(news.id, userId2)

    val result1 = service.find(news.id)
    result1.map(_.usersWhoLike) === Some(Seq(userId1, userId2))
    result1.map(_.likesCount) === Some(2)

    service.updateUserUnlikes(news.id, userId2)

    val result2 = service.find(news.id)
    result2.map(_.usersWhoLike) === Some(Seq(userId1))
    result2.map(_.likesCount) === Some(1)

    service.updateUserUnlikes(news.id, userId1)

    val result3 = service.find(news.id)
    result3.map(_.usersWhoLike) === Some(Seq.empty)
    result3.map(_.likesCount) === Some(0)
  }

  @Test
  def updateAddAndUpdateAndDeleteComment() {
    val f = new NewsFixture
    import f._
    service.save(news)
    service.updateAddComment(news.id, newsComment)
    val result1 = service.find(news.id)
    result1.map(_.comments) === Some(Seq(newsComment))
    result1.map(_.commentsCount) === Some(1)

    val newMessage = "new cool message"
    service.updateNewsCommentMessage(news.id, newsComment.id, newMessage)
    val result2 = service.find(news.id)
    result2.flatMap(_.comments.headOption).map(_.message) == Some(newMessage)

    service.updateDeleteComment(news.id, newsComment.id)

    val result3 = service.find(news.id)
    result3.map(_.comments) === Some(Seq.empty)
    result3.map(_.commentsCount) === Some(0)
  }


  @Test
  def findAllNewsForUser {
    val f = new NewsFixture
    import f._
    val viewedUserId = UserId()
    val newsOfViewedUser = news.copy(id = NewsId(), actorsAndRecipientsIds = Seq(viewedUserId))
    service.save(news)
    service.save(newsOfViewedUser)
    service.findAllNewsForUser(viewedUserId, OffsetLimit(0, 2)) === Seq(newsOfViewedUser)
  }

  @Test
  def updatePhotoAlbumPhotosAndPhotosCounter {
    val f = new NewsFixture
    import f._
    val newPhotoId = PhotoId()
    val newsOfAddedPhoto = news.copy(newsType = NewsTypeId.Photo, photoAlbum = Option(newsPhotoAlbum))
    service.save(news)
    service.save(newsOfAddedPhoto)
    service.updatePhotoAlbumPhotosAndPhotosCounter(newsOfAddedPhoto.id, newPhotoId)
    val result = service.find(news.id).flatMap(_.photoAlbum).map(_.photos)
    result.get === newsPhotoAlbum.photos :+ newPhotoId
  }

  @Test
  def getLastNewsByTypeForActor {
    val news = service.save(new NewsFixture().news)
    service.findLastNewsByTypeForActor(news.actorId.get,news.newsType) === Some(news)
  }

  @Test
  def hideAndRestoreNews() {
    val actor = UserId()
    val recipient = UserId()
    val viewer = UserId()
    var news = service.save(new NewsFixture().news.copy(
      viewers = Seq(viewer), actorId = Option(actor), recipientId = Option(recipient), actorsAndRecipientsIds = Seq(actor, recipient)))

    service.findAllNewsForUser(actor, OffsetLimit()) === Seq(news)
    service.findAllNewsForUser(recipient, OffsetLimit()) === Seq(news)
    service.findAllNews(actor, NewsType.My, OffsetLimit()) === Seq(news)
    service.findAllNews(recipient, NewsType.My, OffsetLimit()) === Seq(news)
    service.findAllNews(viewer, NewsType.All, OffsetLimit()) === Seq(news)

    service.updateUserHideNews(news.id, actor, NewsViewerType.ActorOrRecipient)

    news = news.copy(actorsAndRecipientsIds = Seq(recipient), hidedActorsAndRecipients = Seq(actor))

    service.findAllNewsForUser(actor, OffsetLimit()) === Seq.empty
    service.findAllNewsForUser(recipient, OffsetLimit()).toList === Seq(news)
    service.findAllNews(actor, NewsType.My, OffsetLimit()) === Seq.empty
    service.findAllNews(recipient, NewsType.My, OffsetLimit()) === Seq(news)
    service.findAllNews(viewer, NewsType.All, OffsetLimit()) === Seq(news)

    service.updateUserHideNews(news.id, recipient, NewsViewerType.ActorOrRecipient)

    news = news.copy(actorsAndRecipientsIds = Seq.empty, hidedActorsAndRecipients = Seq(actor, recipient))

    service.findAllNewsForUser(actor, OffsetLimit()) === Seq.empty
    service.findAllNewsForUser(recipient, OffsetLimit()) === Seq.empty
    service.findAllNews(actor, NewsType.My, OffsetLimit()) === Seq.empty
    service.findAllNews(recipient, NewsType.My, OffsetLimit()) === Seq.empty
    service.findAllNews(viewer, NewsType.All, OffsetLimit()) === Seq(news)

    service.updateUserHideNews(news.id, viewer, NewsViewerType.Viewer)

    news = news.copy(viewers = Seq.empty, hidedUsers = Seq(viewer))

    service.findAllNewsForUser(actor, OffsetLimit()) === Seq.empty
    service.findAllNewsForUser(recipient, OffsetLimit()) === Seq.empty
    service.findAllNews(actor, NewsType.My, OffsetLimit()) === Seq.empty
    service.findAllNews(recipient, NewsType.My, OffsetLimit()) === Seq.empty
    service.findAllNews(viewer, NewsType.All, OffsetLimit()) === Seq.empty

    news = news.copy(actorsAndRecipientsIds = Seq(actor, recipient), hidedActorsAndRecipients = Seq.empty, viewers = Seq(viewer), hidedUsers = Seq.empty)

    service.updateUserRestoreNews(news.id, actor, NewsViewerType.ActorOrRecipient)
    service.updateUserRestoreNews(news.id, recipient, NewsViewerType.ActorOrRecipient)
    service.updateUserRestoreNews(news.id, viewer, NewsViewerType.Viewer)

    service.findAllNewsForUser(actor, OffsetLimit()) === Seq(news)
    service.findAllNewsForUser(recipient, OffsetLimit()) === Seq(news)
    service.findAllNews(actor, NewsType.My, OffsetLimit()) === Seq(news)
    service.findAllNews(recipient, NewsType.My, OffsetLimit()) === Seq(news)
    service.findAllNews(viewer, NewsType.All, OffsetLimit()) === Seq(news)
  }
}

class NewsFixture {

  val actorId = UserId()
  val recipientId = UserId()

  val photoId = PhotoId()

  val newsPhotoAlbum = NewsPhotoAlbum(
    photoAlbumId = PhotoAlbumId(),
    photosCount = 1,
    photos = Seq(photoId)
  )

  val news = News(
    viewers = Nil,
    newsType = NewsTypeId.Message,
    createdAt = new Date(),
    actorId = Option(actorId),
    recipientId = Option(recipientId),
    actorsAndRecipientsIds = Seq(actorId, recipientId)
  )

  val newsComment = NewsCommentShort(NewsCommentId(),None, DateHelper.currentDate,"hello", UserId())
}


