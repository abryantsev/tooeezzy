package com.tooe.core.usecase.news

import com.tooe.core.application.Actors
import com.tooe.core.usecase._
import user.UserDataActor
import com.tooe.api._
import com.tooe.api.service.{Photo => _, _}
import com.tooe.core.domain._
import com.tooe.core.util.{Images, Lang}
import com.tooe.core.util.MediaHelper._
import scala.concurrent.Future
import java.util.Date
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.user.response.{Author, ActorAddress}
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.graph.GraphGetFriendsActor
import com.tooe.core.db.graph.msg.{GraphFriends, GraphGetFriends}
import scala.collection.JavaConverters._
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.exceptions.ForbiddenAppException
import com.tooe.core.usecase.wish.WishDataActor
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.db.mongo.util.UnmarshallerEntity

object NewsReadActor {
  final val Id = Actors.NewsRead

  sealed trait NewsAnonymousActorSetting
  object NewsAnonymousActorSetting {
    case object WithDefaultAvatar extends NewsAnonymousActorSetting
    case object WithoutActor extends NewsAnonymousActorSetting
  }

  case class GetAllNews(request: GetAllNewsRequest, currentUserId: UserId, offsetLimit: OffsetLimit)
                       (implicit val lang: Lang, val newsAnonymousActorSetting: NewsAnonymousActorSetting)

  case class GetAllNewsForUser(userId: UserId, currentUserId: UserId, offsetLimit: OffsetLimit)
                              (implicit val lang: Lang, val newsAnonymousActorSetting: NewsAnonymousActorSetting)

  case class GetNews(newsId: NewsId, currentUserId: UserId)
                    (implicit val lang: Lang, val newsAnonymousActorSetting: NewsAnonymousActorSetting)

  case class GetUserCommentNews(userCommentId: NewsId, userId: UserId, lang: Lang)

  case class GetNewsLikes(userId: UserId, newsId: NewsId, offsetLimit: OffsetLimit)
}

class NewsReadActor extends AppActor with ExecutionContextProvider with FriendReadComponent {

  lazy val newsDataActor = lookup(NewsDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val wishDataActor = lookup(WishDataActor.Id)
  lazy val productDataActor = lookup(ProductDataActor.Id)
  lazy val photoAlbumDataActor = lookup(PhotoAlbumDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val maritalStatusActor = lookup(MaritalStatusActor.Id)
  lazy val graphGetFriendsActor = lookup(GraphGetFriendsActor.Id)
  lazy val newsLikeDataActor = lookup(NewsLikeDataActor.Id)

  import NewsReadActor._
  import context.system

  implicit val anonymousActorSettings = new AnonymousNewsActorSettings

  def receive = {
    case msg: GetAllNews =>
      import msg._
      val future = for {
        news <- getAllNews(currentUserId, request.getNewsType, offsetLimit)
        newsResponseData <- getNewsResponseData(news, currentUserId)
      } yield GetAllNewsResponse(newsResponseData, news)
      future pipeTo sender

    case msg: GetAllNewsForUser =>
      import msg._
      val future = for {
        news <- getAllNewsForUser(userId, offsetLimit)
        newsResponseData <- getNewsResponseData(news, currentUserId)
      } yield GetAllNewsResponse(newsResponseData, news)
      future pipeTo sender

    case msg: GetNews =>
      import msg._
      val future = for {
        news <- getNews(newsId)
        newsResponseData <- getNewsResponseData(Seq(news), currentUserId)
      } yield GetNewsResponse(NewsResponseItem(news, newsResponseData))
      future pipeTo sender

    case GetUserCommentNews(userCommentId, userId, lang) =>
      implicit val l = lang
      implicit val newsAnonymousActorSetting = NewsAnonymousActorSetting.WithoutActor //TODO this parameter shouldn't affect the response, but has to be provided
      val future = for {
        news <- getNews(userCommentId)
        _ <- if (news.newsType != NewsTypeId.Message) Future failed ForbiddenAppException("Invalid request parameters")
             else Future successful ()
        newsResponseData <- getNewsResponseData(Seq(news), userId)
      } yield GetCommentResponse(NewsResponseItem(news, newsResponseData))
      future pipeTo sender

    case GetNewsLikes(userId, newsId, offsetLimit) =>
      (for {
        newsLikes <- findNewsLikes(newsId, offsetLimit)
        count <- countNewsLikes(newsId)
        authors <- getAuthors(newsLikes.map(_.userId))
      } yield {
        val items = authors.map(GetNewsLikesResponseItem)
        val selfLiked = authors.find(_.id == userId).map(_.id == userId)
        GetNewsLikesResponse(count, selfLiked, items)
      }).pipeTo(sender)
  }

  def getNewsResponseData(news: Seq[News], currentUserId: UserId)(implicit lang: Lang): Future[NewsResponseData] =
    for {
      usersMap <- getUsersMapFtr(news)
      newsRecipientUsers = newsRecipientsUserMap(news, usersMap)
      maritalStatusInfoMap <- getMaritalStatusInfos(newsRecipientUsers, news, lang)
      userIdMaritalStatusInfoMap = newsRecipientUsers.values.filter(_.maritalStatusId.isDefined).map(user => user.id -> maritalStatusInfoMap(user.maritalStatusId.get)).toMap
      locationsMap <- getLocations(news.flatMap(_.getLocationId))
      productsForPresentsMap <- getProducts(news.flatMap(_.present).map(_.productId))
      wishesMap <- getWishes(news.flatMap(_.getWishId))
      productsForWishesMap <- getProducts(wishesMap.values.map(_.product.productId).toSeq)
      wishProductMap = wishesMap.flatMap {
        case (wid, wish) => Map(wid -> productsForWishesMap(wish.product.productId))
      }
      photoAlbumsMap <- getPhotoAlbums(news.flatMap(_.photoAlbum.map(_.photoAlbumId)))
      photosMap <- getPhotos(news.map(_.photoAlbum.toSeq.flatMap(_.photos)).flatten)
    } yield NewsResponseData(currentUserId, usersMap, userIdMaritalStatusInfoMap,
      locationsMap, productsForPresentsMap, wishesMap, wishProductMap, photoAlbumsMap, photosMap)

  def newsRecipientsUserMap(news: Seq[News], usersMap: Map[UserId, User]): Map[UserId, User] = {
    val recipientIds = news.flatMap(_.recipientId)
    usersMap.filter {
      case (id, _) => recipientIds.contains(id)
    }
  }

  def getUsersMapFtr(news: Seq[News]): Future[Map[UserId, User]] = {
    val usersIds = news.flatMap(n => n.usersWhoLike ++ n.comments.map(_.authorId) ++ n.actorId.toSeq ++ n.recipientId.toSeq)
    userDataActor.ask(UserDataActor.GetUsers(usersIds)).mapTo[Seq[User]].map(_.toMapId(_.id))
  }

  def findNewsLikes(newsId: NewsId, offsetLimit: OffsetLimit) =
    newsLikeDataActor.ask(NewsLikeDataActor.FindAllByNewsId(newsId, offsetLimit)).mapTo[Seq[NewsLike]]

  def countNewsLikes(newsId: NewsId) =
    newsLikeDataActor.ask(NewsLikeDataActor.CountByNewsId(newsId)).mapTo[Long]

  def getAuthors(ids: Seq[UserId]) =
    userReadActor.ask(UserReadActor.SelectAuthors(ids, Images.News.Full.Actor.Media)).mapTo[Seq[Author]]

  def getMaritalStatusInfos(usersMap: Map[UserId, User], news: Seq[News], lang: Lang): Future[Map[MaritalStatusId, MaritalStatusInfo]] = {
    maritalStatusActor.ask(MaritalStatusActor.Find(usersMap.values.flatMap(_.maritalStatusId).toSet, lang))
      .mapTo[Seq[MaritalStatusInfo]].map(_.toMapId(_.id))
  }

  def getMaritalStatusInfo(usersMap: Map[UserId, User], news: News, lang: Lang): Future[Option[MaritalStatusInfo]] = {
    lazy val recipientId = news.recipientId.get
    lazy val recipient = usersMap(recipientId)

    if (news.recipientId.isDefined && recipient.maritalStatusId.isDefined)
      maritalStatusActor.ask(MaritalStatusActor.GetMaritalStatusInfo(recipient.maritalStatusId.get, lang)).mapTo[Option[MaritalStatusInfo]]
    else Future(None)
  }

  def getFriendsIds(userId: UserId): Future[Seq[UserId]] = {
    graphGetFriendsActor.ask(new GraphGetFriends(userId)).mapTo[GraphFriends].map(_.getFriends.asScala.toSeq)
  }

  def getAllNews(userId: UserId, newsType: NewsType, offsetLimit: OffsetLimit): Future[Seq[News]] = {
    newsDataActor.ask(NewsDataActor.GetAllNews(userId, newsType, offsetLimit)).mapTo[Seq[News]]
  }

  def getAllNewsForUser(userId: UserId, offsetLimit: OffsetLimit): Future[Seq[News]] = {
    newsDataActor.ask(NewsDataActor.GetAllNewsForUser(userId, offsetLimit)).mapTo[Seq[News]]
  }

  def getNews(newsId: NewsId): Future[News] = {
    newsDataActor.ask(NewsDataActor.GetNews(newsId)).mapTo[News]
  }

  def getLocations(ids: Seq[LocationId]) =
    locationDataActor.ask(LocationDataActor.FindLocations(ids.toSet)).mapTo[Seq[Location]].map(_.toMapId(_.id))

  def getWishes(ids: Seq[WishId]) =
    wishDataActor.ask(WishDataActor.FindWishes(ids.toSet)).mapTo[Seq[Wish]].map(_.toMapId(_.id))

  def getProducts(ids: Seq[ProductId]) =
    productDataActor.ask(ProductDataActor.GetProducts(ids.distinct)).mapTo[Seq[Product]].map(_.toMapId(_.id))

  def getPhotoAlbums(ids: Seq[PhotoAlbumId]) =
    photoAlbumDataActor.ask(PhotoAlbumDataActor.GetPhotoAlbumsByIds(ids.toSet)).mapTo[Seq[PhotoAlbum]].map(_.toMapId(_.id))

  def getPhotos(ids: Seq[PhotoId]) =
    photoDataActor.ask(PhotoDataActor.GetPhotos(ids.distinct)).mapTo[Seq[Photo]].map(_.toMapId(_.id))
}


case class NewsResponseData(currentUserId: UserId,
                            usersMap: Map[UserId, User],
                            maritalStatusInfoMap: Map[UserId, MaritalStatusInfo],
                            locationsMap: Map[LocationId, Location],
                            productsForPresentsMap: Map[ProductId, Product],
                            wishesMap: Map[WishId, Wish],
                            wishProductsMap: Map[WishId, Product],
                            photoAlbumsMap: Map[PhotoAlbumId, PhotoAlbum],
                            photosMap: Map[PhotoId, Photo])


case class GetNewsLikesResponse(
                                 @JsonProp("likescount") likesCount: Long,
                                 @JsonProp("selfliked") selfLiked: Option[Boolean],
                                 likes: Seq[GetNewsLikesResponseItem]
                                 ) extends SuccessfulResponse

case class GetNewsLikesResponseItem(author: Author) extends UnmarshallerEntity

case class GetCommentResponse(@JsonProp("usercomment") userComment: NewsResponseItem) extends SuccessfulResponse

case class LikeInfo(author: AuthorDetails)

case class CommentInfo
(
  id: NewsCommentId,
  author: AuthorDetails,
  time: Date,
  @JsonProp("msg") message: String
  )

object CommentInfo {
  def apply(comment: NewsCommentShort, user: User): CommentInfo = CommentInfo(
    id = comment.id,
    author = AuthorDetails(user, Images.News.Full.Comments.Media),
    time = comment.createdAt,
    message = comment.message
  )
}

sealed trait NewsActorItem

object NewsActorItem {

  import NewsReadActor.NewsAnonymousActorSetting

  def apply(actorUserOpt: Option[User])
           (implicit
            newsAnonymousActorSetting: NewsAnonymousActorSetting,
            anonymousActorSettings: AnonymousNewsActorSettings): Option[NewsActorItem] =
  {
    actorUserOpt map NewsActorItemFull.apply orElse {
      import NewsAnonymousActorSetting._
      newsAnonymousActorSetting match {
        case WithoutActor      => None
        case WithDefaultAvatar => Some(NewsAnonymousActorItem(anonymousActorSettings.mediaUrl))
      }
    }
  }
}

case class NewsAnonymousActorItem(@JsonProp("media") media: MediaUrl) extends NewsActorItem

case class NewsActorItemFull //TODO similar to ActorItem
(
  @JsonProperty("id") userId: UserId,
  @JsonProperty("name") name: String,
  @JsonProperty("lastname") lastName: String,
  @JsonProperty("media") media: MediaUrl,
  @JsonProperty("address") address: ActorAddress,
  @JsonProperty("gender") gender: Gender,
  star: Option[Boolean]
  ) extends NewsActorItem

object NewsActorItemFull {
  def apply(u: User): NewsActorItem = NewsActorItemFull(
    userId = u.id,
    name = u.name,
    lastName = u.lastName,
    media = u.getMainUserMediaUrl(Images.News.Full.Actor.Media),
    address = ActorAddress(u.contact.address),
    gender = u.gender,
    star = u.isStarOpt
  )
}

case class NewsRecipientItem //TODO similar to ActorItem
(
  @JsonProperty("id") userId: UserId,
  @JsonProperty("name") name: String,
  @JsonProperty("lastname") lastName: String,
  @JsonProperty("media") media: MediaUrl,
  @JsonProperty("address") address: ActorAddress,
  @JsonProperty("maritalstatus") maritalStatus: Option[MaritalStatusInfo],
  @JsonProperty("birthday") birthday: Option[Date],
  star: Option[Boolean],
  gender: Option[Gender]
  )

object NewsRecipientItem {
  def apply(u: User, maritalStatusInfo: Option[MaritalStatusInfo])(implicit lang: Lang): NewsRecipientItem = NewsRecipientItem(
    userId = u.id,
    name = u.name,
    lastName = u.lastName,
    media = u.getMainUserMediaUrl(Images.News.Full.Recepient.Media),
    address = ActorAddress(u.contact.address),
    maritalStatus = maritalStatusInfo,
    birthday = u.birthday,
    star = u.isStarOpt,
    gender = Option(u.gender)
  )
}

case class NewsMessageItem(@JsonProperty("msg") message: String)

case class GetNewsResponse(news: NewsResponseItem) extends SuccessfulResponse

case class GetAllNewsRequest(userGroupId: Option[UsersGroupId]) {
  def getNewsType: NewsType = {
    userGroupId match {
      case Some(UserGroupId.Me) => NewsType.My
      case _ => NewsType.All
    }
  }
}

sealed trait NewsType

object NewsType {

  case object My extends NewsType

  case object All extends NewsType

}

case class GetAllNewsResponse(news: Seq[NewsResponseItem]) extends SuccessfulResponse

object GetAllNewsResponse {
  import NewsReadActor.NewsAnonymousActorSetting

  def apply(newsResponseData: NewsResponseData, news: Seq[News])
           (implicit
            lang: Lang,
            newsAnonymousActorSetting: NewsAnonymousActorSetting,
            anonymousActorSettings: AnonymousNewsActorSettings): GetAllNewsResponse =
    GetAllNewsResponse(news.map(n => NewsResponseItem(n, newsResponseData)))
}

case class NewsResponseItem
(
  id: NewsId,
  time: Date,
  @JsonProp("type") newsType: NewsTypeId,
  @JsonProp("likescount") likesCount: Int,
  @JsonProp("selfliked") selfLiked: Option[Boolean],
  likes: Option[Seq[LikeInfo]],
  @JsonProp("commentscount") commentsCount: Int,
  comments: Option[Seq[CommentInfo]],
  actor: Option[NewsActorItem],
  recipient: Option[NewsRecipientItem],
  wish: Option[NewsWishItem],
  present: Option[NewsPresentItem],
  location: Option[NewsLocationItem],
  photo: Option[NewsPhotoItem],
  @JsonProp("photoalbum") photoAlbum: Option[NewsPhotoAlbumItem],
  message: Option[NewsMessageItem]
  )

object NewsResponseItem {
  import NewsReadActor.NewsAnonymousActorSetting

  def apply(news: News, newsResponseData: NewsResponseData)
           (implicit
            lang: Lang,
            newsAnonymousActorSetting: NewsAnonymousActorSetting,
            anonymousActorSettings: AnonymousNewsActorSettings): NewsResponseItem =
  {
    import newsResponseData._

    val defaultWish = Wish(
      id = WishId(),
      userId = UserId(),
      product = null,
      reasonDate = None
    )
    val defaultProduct = Product(
      id = ProductId(),
      companyId = CompanyId(),
      price = Price(0, CurrencyId("RUR")),
      productTypeId = ProductTypeId.Product,
      name = ObjectMap.empty,
      description = ObjectMap.empty,
      productMedia = Nil,
      location = null,
      regionId = null,
      validityInDays = 30
    )
    val defaultLocation = Location(
      id = LocationId(),
      name = ObjectMap.empty,
      locationMedia = Nil,
      locationsChainId = null,
      companyId = null,
      contact = LocationContact(address = LocationAddress(
        coordinates = Coordinates(),
        regionId = RegionId(),
        regionName = "unknown",
        countryId = CountryId("unknown"),
        country = "unknown",
        street = "unknown"
      ))
    )
    val defaultPhotoAlbum = PhotoAlbum(
      userId = UserId(),
      name = "unknown",
      frontPhotoUrl = MediaObject(PhotoDefaultUrlType.id, Some(UrlType.static))
    )
    val defaultPhoto = Photo(
      photoAlbumId = defaultPhotoAlbum.id,
      userId = UserId(),
      name = Some("unknown"),
      fileUrl = MediaObject(PhotoDefaultUrlType.id, Some(UrlType.static))
    )
    val actorUser = news.actorId flatMap usersMap.get
    NewsResponseItem(
      id = news.id,
      time = news.createdAt,
      newsType = news.newsType,
      likesCount = news.likesCount,
      selfLiked = if (news.usersWhoLike.contains(currentUserId)) Some(true) else None,
      likes = if (news.usersWhoLike.nonEmpty)
        Some(usersMap.values.toSeq.filter(user => news.usersWhoLike.contains(user.id)).map(user => LikeInfo(AuthorDetails(user, Images.News.Full.Likes.Media))))
      else None,
      commentsCount = news.commentsCount,
      comments = if (news.comments.nonEmpty) Some(news.comments.map(c =>
        CommentInfo(c, usersMap(c.authorId))
      ))
      else None,
      actor = NewsActorItem(actorUser),
      recipient = news.recipientId.map(recipientId =>
        NewsRecipientItem(usersMap(recipientId), maritalStatusInfoMap.get(recipientId))(lang)
      ),
      message = news.getMessage.map(m => NewsMessageItem(m)),
      wish = news.getWishId.map(id => wishesMap.get(id).getOrElse(defaultWish)).map(wish => NewsWishItem(wish, wishProductsMap.get(wish.id).getOrElse(defaultProduct))),
      present = news.getProductId.map(pid => productsForPresentsMap.get(pid).getOrElse(defaultProduct)).map(p => NewsPresentItem(p, news)),
      location = news.getLocationId.map(id => locationsMap.get(id).getOrElse(defaultLocation)).map(l => NewsLocationItem(l)),
      photo = Option(news.newsType).filter(_ == NewsTypeId.Photo).flatMap {
        _ =>
          news.photoAlbum.map(pa => NewsPhotoItem(pa, photoAlbumsMap.get(pa.photoAlbumId).getOrElse(defaultPhotoAlbum), photosMap.withDefaultValue(defaultPhoto)))
      },
      photoAlbum = Option(news.newsType).filter(_ == NewsTypeId.PhotoAlbum).flatMap {
        _ => news.photoAlbum.map(pa => NewsPhotoAlbumItem(pa, photoAlbumsMap.withDefaultValue(defaultPhotoAlbum)(pa.photoAlbumId), photosMap.withDefaultValue(defaultPhoto)))
      }
    )
  }
}

case class NewsWishItem
(
  id: WishId,
  name: String,
  description: String,
  time: Option[Date],
  media: MediaUrl,
  @JsonProp("productid") productId: ProductId
  )

object NewsWishItem {

  def apply(w: Wish, p: Product)(implicit lang: Lang): NewsWishItem =
    NewsWishItem(
      id = w.id,
      name = p.name.localized getOrElse "",
      description = p.description.localized getOrElse "",
      time = w.reasonDate,
      media = p.productMedia.headOption.map(_.media).asMediaUrl(Images.News.Full.Wish.Media, ProductDefaultUrlType),
      productId = p.id
    )
}

case class NewsPresentItem
(
  id: ProductId,
  name: String,
  media: MediaUrl,
  message: Option[String]
  )

object NewsPresentItem {
  def apply(product: Product, news: News)(implicit lang: Lang): NewsPresentItem =
    NewsPresentItem(product.id,
      product.name.localized.getOrElse(""),
      product.productMedia.headOption.map(_.media).asMediaUrl(Images.News.Full.Present.Media, ProductDefaultUrlType),
      message = news.present.flatMap(_.message))
}

case class NewsLocationItem
(
  id: LocationId,
  name: String,
  media: MediaUrl,
  address: LocationAddressItem,
  @JsonProp("coords") coordinates: Coordinates
  )

object NewsLocationItem {

  def apply(l: Location)(implicit lang: Lang): NewsLocationItem =
    NewsLocationItem(l.locationId,
      l.name.localized.getOrElse(""),
      l.locationMedia.headOption.map(_.url).asMediaUrl(Images.News.Full.Location.Media, LocationDefaultUrlType),
      LocationAddressItem(l.contact.address),
      l.contact.address.coordinates)
}

case class NewsPhotoItem
(
  id: PhotoAlbumId,
  name: String,
  @JsonProp("photoscount") photosCount: Int,
  photos: Seq[PhotoIdWithUrl]
  )

object NewsPhotoItem {

  def apply(pa: NewsPhotoAlbum, photoAlbum: PhotoAlbum, photosMap: Map[PhotoId, Photo]): NewsPhotoItem =
    NewsPhotoItem(
      id = pa.photoAlbumId,
      name = photoAlbum.name,
      photosCount = pa.photos.size,
      photos = pa.photos.map(photoId => PhotoIdWithUrl(photoId, photosMap(photoId).fileUrl.asMediaUrl(Images.News.Full.Photo.Main)))
    )
}

case class NewsPhotoAlbumShortItem
(
  id: PhotoAlbumId,
  name: String,
  media: MediaUrl
  )

case class NewsPhotoAlbumItem
(
  id: PhotoAlbumId,
  name: String,
  @JsonProp("photoscount") photosCount: Int,
  photos: Seq[PhotoIdWithUrl]
  )

object NewsPhotoAlbumItem {

  def apply(pa: NewsPhotoAlbum, photoAlbum: PhotoAlbum, photosMap: Map[PhotoId, Photo]): NewsPhotoAlbumItem =
    NewsPhotoAlbumItem(
      id = pa.photoAlbumId,
      name = photoAlbum.name,
      photosCount = pa.photosCount,
      photos = pa.photos.map(photoId => PhotoIdWithUrl(photoId, photosMap(photoId).fileUrl.asMediaUrl(Images.News.Full.Photoalbum.Photos)))
    )
}

case class PhotoIdWithUrl(id: PhotoId, media: MediaUrl)

