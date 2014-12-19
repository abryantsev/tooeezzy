package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import com.tooe.core.domain._
import com.tooe.core.util.{DateHelper, Lang}

@Document(collection = "check_in")
case class Checkin
(
  id: CheckinId = CheckinId(),
  creationTime: Date = DateHelper.currentDate,
  user: UserInfoCheckin,
  location: LocationInfoCheckin,
  friends: Seq[UserId]
  )
  extends CheckinBaseProjection
{
  def coords = location.coordinates
}

trait CheckinBaseProjection {
  def id: CheckinId
  def creationTime: Date
  def user: UserInfoCheckin
  def location: LocationInfoCheckin
}

case class UserInfoCheckin
(
  userId: UserId,
  name: String,
  lastName: String,
  secondName: Option[String] = None,
  media: Option[MediaObject],
  gender: Gender
  )

object UserInfoCheckin {
  def apply(user: User): UserInfoCheckin = UserInfoCheckin(
    user.id,
    user.name,
    user.lastName,
    user.secondName,
    user.getMainUserMediaOpt.map(m => m.url),
    user.gender
  )
}

//TODO optional parameters?
case class LocationInfoCheckin(
                                locationId: LocationId = null,
                                coordinates: Coordinates = null,
                                openingHours: String = null,
                                name: String = null,
                                address: LocationAddressItem = null,
                                media: Option[MediaObject] = None
                                )

object LocationInfoCheckin {
  def apply(l: Location)(implicit lang: Lang): LocationInfoCheckin = LocationInfoCheckin(
    locationId = l.id,
    coordinates = l.contact.address.coordinates,
    openingHours = l.openingHours.localized getOrElse "",
    name = l.name.localized getOrElse "",
    address = LocationAddressItem(l.contact.address),
    media = l.getMainLocationMediaOpt.map(m => m.url)
  )
}

object Checkin {

  def apply(location: Location, user: User, friends: Seq[UserId])(implicit lang: Lang): Checkin = Checkin(
    user = UserInfoCheckin(user),
    location = LocationInfoCheckin(location),
    friends = friends
  )
}