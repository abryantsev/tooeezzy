package com.tooe.core.usecase

import checkin.CheckinDataActor
import com.tooe.core.db.mongo.domain._
import com.tooe.core.application.Actors
import com.tooe.api.service.{ExecutionContextProvider, SuccessfulResponse}
import com.tooe.core.usecase.user.UserDataActor
import location.LocationDataActor
import concurrent.Future
import com.tooe.core.util.Lang
import com.tooe.api._
import com.tooe.core.domain.{UrlType, UserId, LocationId}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.urls.{UrlsDataActor, UrlsWriteActor}
import spray.http.StatusCodes

object CheckinWriteActor {
  final val Id = Actors.CheckinWrite

  case class DoCheckin(request: CheckinRequest, userId: UserId, lang: Lang)
}

class CheckinWriteActor extends AppActor with ExecutionContextProvider with FriendReadComponent {

  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val checkinDataActor = lookup(CheckinDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val urlsDataActor = lookup(UrlsDataActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)

  import com.tooe.core.usecase.CheckinWriteActor._

  def receive = {
    case DoCheckin(request: CheckinRequest, userId, lang) =>
      val future = for {
        _ <- preventDoubleCheckIn(request.locationId, userId)(lang)
        location <- getLocation(request.locationId)
        user <- getUser(userId)
        friends <- getUserFriends(userId)
        checkin <- {
          removeUserCheckins(user.id)
          saveCheckin(Checkin(location, user, friends.toSeq)(lang))
        }.mapTo[Checkin]
      } yield {
        newsWriteActor ! NewsWriteActor.AddCheckinNews(userId, location.id)
        location.getMainLocationMediaOpt.filter(_.url.mediaType.exists(_ == UrlType.s3)).foreach {
           media => urlsWriteActor ! UrlsWriteActor.AddCheckinLocationMedia(checkin.id, media.url.url)
        }
        user.getMainUserMediaOpt.filter(_.url.mediaType.exists(_ == UrlType.s3)).foreach {
           media => urlsWriteActor ! UrlsWriteActor.AddCheckinUserMedia(checkin.id, media.url.url)
        }
        SuccessfulResponse
      }
      future pipeTo sender
  }

  def preventDoubleCheckIn(locationId: LocationId, userId: UserId)(implicit lang: Lang): Future[_] =
    findCheckIn(userId) flatMap {
      case Some(checkIn) if checkIn.location.locationId == locationId =>
        infoMessageActor ? InfoMessageActor.GetFailure("double_check_in_is_not_allowed", lang, StatusCodes.Conflict)
      case _ => Future successful ()
    }

  def findCheckIn(userId: UserId): Future[Option[Checkin]] =
    (checkinDataActor ? CheckinDataActor.FindCheckinByUserId(userId)).mapTo[Option[Checkin]]

  def getLocation(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetLocation(id)).mapTo[Location]

  def getUser(id: UserId): Future[User] = (userDataActor ? UserDataActor.GetUser(id)).mapTo[User]

  def removeUserCheckins(id: UserId): Unit = {
    (checkinDataActor ? CheckinDataActor.GetCheckinByUserId(id)).mapTo[Checkin].map { checkin =>
      checkinDataActor ! CheckinDataActor.RemoveUserCheckins(id)
      val urlsRemoveData = checkin.location.media.map { m => id.id -> m.url } :: checkin.user.media.map { m => id.id -> m.url } :: Nil
      urlsDataActor ! UrlsDataActor.DeleteUrlsByEntityAndUrl(urlsRemoveData.flatten)
    }
  }

  def saveCheckin(checkin: Checkin): Future[Checkin] =
    (checkinDataActor ? CheckinDataActor.SaveCheckin(checkin)).mapTo[Checkin]

}

case class CheckinRequest(@JsonProp("locationid") locationId: LocationId) extends UnmarshallerEntity