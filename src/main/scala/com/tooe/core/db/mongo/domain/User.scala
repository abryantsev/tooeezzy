package com.tooe.core.db.mongo.domain

import com.tooe.core.domain._
import com.tooe.core.usecase.ImageInfo
import com.tooe.core.usecase.ImageType
import com.tooe.core.util.Lang
import com.tooe.core.util.MediaHelper._
import java.util.Date
import org.springframework.data.mongodb.core.mapping.Document
import scala.Some
import com.tooe.core.db.mongo.domain.UserMedia.{CDNDynamic, CDNType}

trait UserOnlineStatus {
  def id: UserId
  def onlineStatus: Option[OnlineStatusId]
  def getOnlineStatus: OnlineStatusId = onlineStatus getOrElse UserOnlineStatus.defaultOnlineStatus
}

trait UserMediaHolder {
  def userMedia: Seq[UserMedia]
  def userPhotoMedia: Seq[UserMedia]
  def findAvatarMedia: Seq[UserMedia]
  def videoMedia: Option[UserMedia]
  def backgroundMedia: Option[UserMedia]
  def isLimitOfAvatarMediaReached: Boolean
  def ifAvatarExists(url: String): Either[Unit, UserMedia]
  def optimisticLock: Int
}

object UserOnlineStatus {
  def defaultOnlineStatus: OnlineStatusId = OnlineStatusId.Online
}

/**
 * @param maritalStatusId family/marital status in db.maritalstatus
 * @param photoAlbums db.photoalbum
 * @param wishes db.wish
 * @param gifts user has given to friends in db.present
 */
@Document( collection = "user" )
case class User
(
  id: UserId = UserId(),
  name: String,
  lastName: String,
  secondName: Option[String] = None,
  birthday: Option[Date],
  gender: Gender,
  maritalStatusId: Option[MaritalStatusId] = None,
  onlineStatus: Option[OnlineStatusId] = None,
  contact: UserContact,
  details: UserDetails,
  star: Option[UserStar] = None,
  settings: UserSettings,
  userMedia: Seq[UserMedia] = Nil,
  photoAlbums: Seq[PhotoAlbumId] = Nil,
  lastPhotos: Seq[PhotoId] = Nil,
  wishes: Seq[WishId] = Nil,
  gifts: Seq[PresentId] = Nil,
  statistics: UserStatistics,
  optimisticLock: Int = 0
  ) extends UserOnlineStatus with UserMediaHolder {

  lazy val names = (Seq(name, lastName) ++ secondName).map(_.trim.toLowerCase).permutations.map(_.mkString(" ")).toSeq

  def getMainAvatarLink(imageSize: String) = getMainUserMediaOpt.map(_.url).asUrl(imageSize, UserDefaultUrlType(this.gender))
  def toImageInfo(imageSize: String) = ImageInfo(getMainAvatarLink(imageSize), ImageType.avatar, id.id)
  def getMainUserMediaOpt = userPhotoMedia.find(_.purpose == Some("main"))
  def findAvatarMedia = userPhotoMedia.filter(um => um.purpose == Some("main") | um.purpose == None)
  def getMainUserMediaUrl(imageSize: String) = getMainUserMediaOpt.map(_.url).asMediaUrl(imageSize, UserDefaultUrlType(this.gender))
  def isStar = star.isDefined
  def isStarOpt = if(isStar) Some(isStar) else None
  def userPhotoMedia = userMedia.filter(_.mediaType == "f")
  def birthdayRepr = if(settings.showAge) birthday else None
  def isLimitOfAvatarMediaReached = findAvatarMedia.size >= 5
  def videoMedia = userMedia.find(_.mediaType == "v")
  def backgroundMedia = userMedia.find(_.purpose == Some("bg"))
  def ifAvatarExists(url: String) = findAvatarMedia.find(_.url.url.id == url).toRight({})
  def fullName = name + " " + lastName
}

case class UserStar(starCategoryIds: Seq[StarCategoryId],
                    presentMessage: Option[String] = None,
                    agentId: Option[AdminUserId],
                    subscribersCount: Int)

case class UserContact
(
  address: UserAddress,
  phones: UserPhones ,
  email: String = ""
)

object UserContact {
  def forRegistration(country: Country, region: Region, phone: Option[Phone], email: String)
                     (implicit lang: Lang): UserContact =
    UserContact(
      address = UserAddress(country, region),
      phones = UserPhones(all = phone map { p => Seq(p) }, main = phone),
      email = email
    )
}

case class UserDetails
(
  defaultCurrency: CurrencyId,
  registrationTime: Date,
  education: Option[String],
  job: Option[String],
  aboutMe: Option[String]
  )
object UserDetails {
  def forRegistration(country: Country): UserDetails = UserDetails(
    defaultCurrency = country.defaultCurrency,
    registrationTime = new Date,
    education = None,
    job = None,
    aboutMe = None
  )
}

case class UserSettings
(
  pageRights: Seq[String], // page rights: show page to db.usersgroup
  mapRights: Seq[String], // map rights: show favorite locations to db.usersgroup-s  ?? the same as for page rights?
  showAge: Boolean, // show age
  messageSettings: UserMessageSetting
  )

object UserSettings {
  def forRegistration: UserSettings = UserSettings(
    pageRights = Nil,
    mapRights = Nil,
    showAge = true,
    messageSettings = UserMessageSetting(None,None,None)
  )
}

case class UserMedia
(
  url: MediaObject,
  description: Option[String] = None,
  mediaType: String = "f",
  purpose: Option[String] = None,
  videoFormat: Option[String] = None,
  descriptionStyle: Option[String] = None,
  descriptionColor: Option[String] = None,
  cdnType: CDNType = CDNDynamic
) {
  def responsePurpose: Option[String] = purpose match {
    case Some("bg") => Some("background")
    case elze => elze
  }

  def isMain: Boolean = purpose map (_ == "main") getOrElse false
}
object UserMedia {
  trait CDNType
  case object CDNDynamic extends CDNType
  case object CDNStatic extends CDNType
  def mainFromUrl(url: String): UserMedia = UserMedia(purpose = Some("main"), url = MediaObject(url), mediaType = "f")
  def avatarFromUrl(url: String): UserMedia = UserMedia(url = MediaObject(url), mediaType = "f")
}

case class UserAddress
(
  countryId: CountryId,
  regionId: RegionId,
  regionName: String,
  country: String
  )
object UserAddress {
  def apply(country: Country, region: Region)(implicit lang: Lang): UserAddress = UserAddress(
    countryId = country.id,
    regionId = region.id,
    regionName = region.name.localized getOrElse "",
    country = country.name.localized getOrElse ""
  )
}

case class UserPhones(all: Option[Seq[Phone]], main: Option[Phone]) {
  def allShort: Seq[PhoneShort] = all getOrElse Nil map (_.toPhoneShort)
  def mainShort: Option[PhoneShort] = main map (_.toPhoneShort)
  def additionalShort: Seq[PhoneShort] = (allShort.toSet -- mainShort.toSet).toSeq
  def additionalShortOpt: Option[Seq[PhoneShort]] = if (additionalShort.isEmpty) None else Some(additionalShort)
}

case class UserMessageSetting
(
  showMessageText: Option[Boolean], //show message text
  soundsEnabled: Option[Boolean], //audio/sounds are on if message arrived
  sendEmailEvent: Option[UserSendEmailEvent]) // purpose

case class UserSendEmailEvent(email: String, events: Seq[String])

case class UserStatistics(
  presentsCount: Int = 0,
  certificatesCount: Int = 0,
  sentPresentsCount: Int = 0,
  sentCertificatesCount: Int = 0,
  friendsCount: Int = 0,
  friendsRequestCount: Int = 0,
  wishesCount: Int = 0,
  fulfilledWishesCount: Int = 0,
  favoriteLocationsCount: Int = 0,
  starSubscriptionsCount: Int = 0,
  locationSubscriptionsCount: Int = 0,
  photoAlbumsCount: Int = 0,
  eventsCount: Int = 0,
  newStatistic: UserStatisticsNew = UserStatisticsNew()
)

case class UserStatisticsNew(
  presentsCount: Int = 0,
  eventsCount: Int = 0
)