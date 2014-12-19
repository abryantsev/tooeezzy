package com.tooe.core.db.mongo.domain

import org.bson.types.ObjectId
import com.tooe.core.db.mongo.util.{PermutationHelper, UnmarshallerEntity}
import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain._
import com.tooe.core.domain.MediaObject
import com.tooe.core.domain.UserId
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.LocationCategoryId
import com.tooe.core.domain.AdditionalLocationCategoryId
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.LocationsChainId
import scala.Some
import com.tooe.core.domain.LocationPhotoId
import com.tooe.core.domain.RegionId
import com.tooe.core.util.MediaHelper.{LocationDefaultUrlType, OptMediaObjectImplicits}

@Document(collection = "location")
case class Location
(
  id: LocationId = LocationId(new ObjectId),
  companyId: CompanyId,
  locationsChainId: Option[LocationsChainId],
  brandId: Option[ObjectId] = None,
  name: ObjectMap[String] = ObjectMap.empty,
  description: ObjectMap[String] = ObjectMap.empty,
  openingHours: ObjectMap[String] = ObjectMap.empty,
  contact: LocationContact,
  locationCategories: Seq[LocationCategoryId] = Nil,
  additionalLocationCategories: Seq[AdditionalLocationCategory] = Nil,
  hasPromotions: Option[Boolean] = None,
  locationMedia: Seq[LocationMedia] = Nil,
  photoAlbums: Seq[ObjectId] = Nil,
  lastPhotos: Seq[LocationPhotoId] = Nil,
  usersWhoFavorite: Seq[UserId] = Nil,
  statistics: LocationCounters = LocationCounters(),
  lifecycleStatusId: Option[LifecycleStatusId] = None,
  specialRole: Option[LocationSpecialRole] = None
  ) extends UnmarshallerEntity {

  lazy val names = name.values.flatMap(PermutationHelper.permuteBySpace).toSeq

  def locationId = id

  def getMainLocationMediaOpt = locationMedia.find(_.purpose == Some("main")).headOption

  def getMainLocationMediaUrl(imageSize: String) = getMainLocationMediaOpt.map(_.url).asMediaUrl(imageSize, LocationDefaultUrlType)

  def oneOfLocationCategoryId = locationCategories.headOption

  @deprecated("use contact.mainPhone")
  def mainPhone = contact.mainPhone
}

case class LocationContact
(
  address: LocationAddress,
  phones: Seq[Phone] = Nil,
  url: Option[String] = None
) {

  def mainPhone = phones.find(_.purpose == Some("main"))

  def activationPhone = phones.find(_.purpose == Some("activation"))

}

case class LocationAddress
(
  coordinates: Coordinates,
  regionId: RegionId,
  regionName: String,
  countryId: CountryId,
  country: String,
  street: String
)

case class AdditionalLocationCategory(id: AdditionalLocationCategoryId, name: ObjectMap[String])

case class LocationMedia
(
  url: MediaObject,
  description: Option[String] = None,
  mediaType: String = "",
  purpose: Option[String] = None
)

case class LocationCounters(
  presentsCount: Int = 0,
  photoalbumsCount: Int = 0,
  reviewsCount: Int = 0,
  favoritePlaceCount: Int = 0,
  subscribersCount: Int = 0,
  countOfCheckins: Int = 0,
  productsCount: Int = 0
)

case class LocationSpecialRole(id: String)
object LocationSpecialRole {
  val Tooe = LocationSpecialRole("tooeezzy")
}