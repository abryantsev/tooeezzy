package com.tooe.core.usecase

import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.service._
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.usecase.location_news.{LocationNewsLikeWriteActor, LocationNewsLikeDataActor, LocationNewsDataActor}
import com.tooe.core.util.{Images, Lang}
import java.util.Date
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.LocationNewsLike
import com.tooe.api.service.ChangeLocationNewsRequest
import com.tooe.api.service.AddLocationNewsRequest
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.usecase.location.LocationDataActor.FindLocations
import com.tooe.api.service.RouteContext
import com.tooe.core.domain.LocationsChainId
import com.tooe.core.usecase.UserReadActor.GetAuthorDetailsByIds
import com.tooe.core.usecase.location_news.LocationNewsLikeDataActor.GetUserLocationNewsLikesByNews
import com.tooe.core.domain.LocationId
import com.tooe.core.usecase.location_news.LocationNewsDataActor.RemoveLocationNews
import com.tooe.core.domain.LocationNewsId
import com.tooe.core.domain.UserId
import com.tooe.core.domain.MediaUrl
import com.tooe.core.db.mongo.domain.LocationNews
import com.tooe.core.usecase.location_news.LocationNewsDataActor.UpdateLocationNews
import com.tooe.core.usecase.location_news.LocationNewsDataActor.CreateLocationNews

object LocationNewsActor {
  final val Id = Actors.LocationNews

  case class AddLocationNews(request: AddLocationNewsRequest, ctx: RouteContext)
  case class DeleteLocationNews(locationNewsId: LocationNewsId)
  case class ChangeLocationNews(locationNewsId: LocationNewsId, request: ChangeLocationNewsRequest, routeContext: RouteContext)
  case class GetLocationNews(locationId: LocationId, userId: Option[UserId], showType: ShowType, offsetLimit: OffsetLimit, routeContext: RouteContext)
  case class LikeLocationNews(userId: UserId, locationNewsId: LocationNewsId)
  case class UnlikeLocationNews(userId: UserId, locationNewsId: LocationNewsId)
  case class GetLocationsChainNews(locationsChainId: LocationsChainId, userId: UserId, offsetLimit: OffsetLimit, routeContext: RouteContext)
}

//TODO there should be two actors Write and Read
class LocationNewsActor extends AppActor with ExecutionContextProvider {

  lazy val locationNewsDataActor = lookup(LocationNewsDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)
  lazy val locationNewsLikeDataActor = lookup(LocationNewsLikeDataActor.Id)
  lazy val locationNewsLikeWriteActor = lookup(LocationNewsLikeWriteActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)

  import LocationNewsActor._

  def receive = {

    case AddLocationNews(request, ctx) =>
      implicit val lang = ctx.lang
      val future = for {
        location <- getLocation(request.locationId)
        news = LocationNews(
          content = ObjectMap(request.content),
          commentsEnabled = request.enableComments,
          locationId = request.locationId,
          locationsChainId = location.locationsChainId
        )
        locationNews <- (locationNewsDataActor ? CreateLocationNews(news)).mapTo[LocationNews]
      } yield {
        if(location.specialRole.exists(_ == LocationSpecialRole.Tooe))
          newsWriteActor ! NewsWriteActor.AddSpecialLocationNews(locationNews, lang)
        else
          newsWriteActor ! NewsWriteActor.AddLocationNews(locationNews, lang)
        LocationNewsCreatedResponse(LocationNewsIdCreated(locationNews.id))
      }
      future pipeTo sender

    case DeleteLocationNews(locationNewsId) =>
      locationNewsDataActor ! RemoveLocationNews(locationNewsId)
      sender ! SuccessfulResponse

    case ChangeLocationNews(locationNewsId, request, routeContext) =>
      locationNewsDataActor ! UpdateLocationNews(locationNewsId, request, routeContext.lang)
      sender ! SuccessfulResponse

    case GetLocationNews(locationId, userId, showType, offsetLimit, routeContext) =>
      implicit val lang: Lang = routeContext.lang
      val result = for {
        (news, newsCount) <- (locationNewsDataActor ? LocationNewsDataActor.GetLocationNews(locationId, offsetLimit))
                            .zip(locationNewsDataActor ? LocationNewsDataActor.GetLocationNewsCount(locationId)).mapTo[(Seq[LocationNews], Long)]
        (authors, selfLikedNews) <- getAuthorsDetails(news, showType).zip(getUserLikes(news, userId, showType)).mapTo[(Seq[AuthorDetails], Seq[LocationNewsLike])]
        authorsMap = authors.map(a => (a.id, a)).toMap
      } yield {
        showType match {
          case ShowType.Adm =>  GetLocationNewsAdmResponse(newsCount, news map (LocationNewsAdmItem(_)))
          case _ => GetLocationNewsFullResponse(newsCount, news map (LocationNewsItem(_, authorsMap, selfLikedNews)))
        }
      }

      result pipeTo sender

    case LikeLocationNews(userId, locationNewsId) =>
      locationNewsLikeWriteActor ! LocationNewsLikeWriteActor.LikeLocationNews(locationNewsId, userId)
      sender ! SuccessfulResponse

    case UnlikeLocationNews(userId, locationNewsId) =>
      (for {
        _ <- locationNewsLikeWriteActor ? LocationNewsLikeWriteActor.UnlikeLocationNews(userId, locationNewsId)
        lastLikes <- (locationNewsLikeDataActor ? LocationNewsLikeDataActor.GetLastLikes(locationNewsId)).mapTo[Seq[LocationNewsLike]]
      } yield {
        locationNewsDataActor ! LocationNewsDataActor.UnlikeLocationNews(lastLikes.map(_.userId), locationNewsId)
      })
      sender ! SuccessfulResponse

    case GetLocationsChainNews(locationsChainId, userId, offsetLimit, routeContext) =>
      (for {
        (news, newsCount) <- (locationNewsDataActor ? LocationNewsDataActor.GetLocationsChainNews(locationsChainId, offsetLimit))
              .zip(locationNewsDataActor ? LocationNewsDataActor.GetLocationsChainNewsCount(locationsChainId)).mapTo[(Seq[LocationNews], Long)]
        locations <- (locationDataActor ? FindLocations(news.map(_.locationId).toSet)).mapTo[Seq[Location]]
        locationsMap = locations.map(l => (l.id, l)).toMap
        (authors, selfLikedNews) <- getAuthorsDetails(news).zip(getUserLikes(news, Some(userId))).mapTo[(Seq[AuthorDetails], Seq[LocationNewsLike])]
        authorsMap = authors.map(a => (a.id, a)).toMap
      } yield {
        implicit val lang: Lang = routeContext.lang
        GetLocationChainNewsFullResponse(newsCount, news map (LocationChainNewsItem(_, authorsMap, selfLikedNews, locationsMap)))
      }) pipeTo sender

  }


  def getAuthorsDetails(news: Seq[LocationNews], showType: ShowType = ShowType.None): Future[Seq[AuthorDetails]] = {
    if(showType != ShowType.Adm)
      (userReadActor ? GetAuthorDetailsByIds(news.map(_.lastLikes).flatten.toSet.toSeq, Images.Locationnews.Full.Like.Author)).mapTo[Seq[AuthorDetails]]
    else
      Future successful Nil
  }

  def getUserLikes(news: Seq[LocationNews], userIdOpt: Option[UserId], showType: ShowType = ShowType.None): Future[Seq[LocationNewsLike]] =
    userIdOpt map { userId =>
      (locationNewsLikeDataActor ? GetUserLocationNewsLikesByNews(news.map(_.id), userId)).mapTo[Seq[LocationNewsLike]]
    } getOrElse (Future successful Nil)

  def getLocation(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetLocation(id)).mapTo[Location]
}

case class LocationNewsCreatedResponse(@JsonProperty("locationnews") locationNews: LocationNewsIdCreated) extends SuccessfulResponse

case class LocationNewsIdCreated(id: LocationNewsId)

case class GetLocationNewsAdmResponse(@JsonProperty("locationnewscount") count: Long,
                                      @JsonProperty("locationnews") news: Seq[LocationNewsAdmItem]) extends SuccessfulResponse

case class LocationNewsAdmItem(id: LocationNewsId, time: Date, content: String)

object LocationNewsAdmItem {

  def apply(locationNews: LocationNews)(implicit lang: Lang): LocationNewsAdmItem =
    LocationNewsAdmItem(locationNews.id, locationNews.createdTime, locationNews.content.localized getOrElse "")

}

case class GetLocationNewsFullResponse(@JsonProperty("locationnewscount") count: Long,
                                      @JsonProperty("locationnews") news: Seq[LocationNewsItem]) extends SuccessfulResponse


case class LocationNewsItem(id: LocationNewsId,
                            time: Date,
                            content: String,
                            @JsonProperty("likescount") likesCount: Long,
                            @JsonProperty("selfliked") selfLiked: Option[Boolean],
                            likes: Seq[LocationNewsLikeItem])

case class LocationNewsLikeItem(author: AuthorDetails)

object LocationNewsItem {

  def apply(locationNews: LocationNews, authors: Map[UserId, AuthorDetails], selfLikedNews: Seq[LocationNewsLike])(implicit lang: Lang): LocationNewsItem =
    LocationNewsItem(
      id = locationNews.id,
      time = locationNews.createdTime,
      content = locationNews.content.localized getOrElse "",
      likesCount = locationNews.likesCount,
      selfLiked = if(selfLikedNews.exists(p => p.locationNewsId == locationNews.id)) Some(true) else None,
      likes = locationNews.lastLikes.map(userId => LocationNewsLikeItem(authors(userId)))
    )

}

case class GetLocationChainNewsFullResponse(@JsonProperty("locationnewscount") count: Long,
                                       @JsonProperty("locationnews") news: Seq[LocationChainNewsItem]) extends SuccessfulResponse


case class LocationChainNewsItem(id: LocationNewsId,
                            time: Date,
                            content: String,
                            @JsonProperty("likescount") likesCount: Long,
                            @JsonProperty("selfliked") selfLiked: Option[Boolean],
                            likes: Seq[LocationNewsLikeItem],
                            location: LocationShortInfoItem)

object LocationChainNewsItem {

  def apply(locationNews: LocationNews, authors: Map[UserId, AuthorDetails], selfLikedNews: Seq[LocationNewsLike], locations: Map[LocationId, Location])(implicit lang: Lang): LocationChainNewsItem = {
    val location = locations(locationNews.locationId)

    LocationChainNewsItem(
      id = locationNews.id,
      time = locationNews.createdTime,
      content = locationNews.content.localized getOrElse "",
      likesCount = locationNews.likesCount,
      selfLiked = if(selfLikedNews.exists(p => p.locationNewsId == locationNews.id)) Some(true) else None,
      likes = locationNews.lastLikes.map(userId => LocationNewsLikeItem(authors(userId))),
      location = LocationShortInfoItem(
        location.id,
        location.getMainLocationMediaUrl(Images.Locationnews.Full.Location.Media),
        location.name.localized.getOrElse(""))
    )
  }
}

case class LocationShortInfoItem(id: LocationId,
                                 @JsonProperty("mainmedia") media: MediaUrl,
                                 name: String)
