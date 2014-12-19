package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.{WritingConverter, ReadingConverter}
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import java.util.Date

@WritingConverter
class UserWriteConverter extends Converter[User, DBObject] with UserConverter {
  def convert(obj: User) = UserConverter.serialize(obj)
}

@ReadingConverter
class UserReadConverter extends Converter[DBObject, User] with UserConverter {
  def convert(source: DBObject) = UserConverter.deserialize(source)
}

trait UserConverter
  extends UserContactConverter
  with UserDetailsConverter
  with UserStarConverter
  with UserSettingsConverter
  with UserStatisticsConverter
  with UserMediaConverter
{
  import DBObjectConverters._

  implicit val UserConverter = new DBObjectConverter[User] {
    def serializeObj(obj: User) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("ln").value(obj.lastName)
      .field("sn").value(obj.secondName)
      .field("ns").value(obj.names)
      .field("bd").value(obj.birthday)
      .field("g").value(obj.gender)
      .field("fs").value(obj.maritalStatusId)
      .field("os").value(obj.onlineStatus)
      .field("c").value(obj.contact)
      .field("d").value(obj.details)
      .field("star").value(obj.star)
      .field("s").value(obj.settings)
      .field("um").value(obj.userMedia)
      .field("lp").value(obj.lastPhotos)
      .field("pa").value(obj.photoAlbums)
      .field("ws").value(obj.wishes)
      .field("gs").value(obj.gifts)
      .field("st").value(obj.statistics)
      .field("ol").value(obj.optimisticLock)

    def deserializeObj(source: DBObjectExtractor) = User(
      id = source.id.value[UserId],
      name = source.field("n").value[String],
      lastName = source.field("ln").value[String],
      secondName = source.field("sn").opt[String],
      birthday = source.field("bd").opt[Date],
      gender = source.field("g").value[Gender],
      maritalStatusId = source.field("fs").opt[MaritalStatusId],
      onlineStatus = source.field("os").opt[OnlineStatusId],
      contact = source.field("c").value[UserContact],
      details = source.field("d").value[UserDetails],
      star = source.field("star").opt[UserStar],
      settings = source.field("s").value[UserSettings],
      userMedia = source.field("um").seq[UserMedia],
      photoAlbums = source.field("pa").seq[PhotoAlbumId],
      lastPhotos = source.field("lp").seq[PhotoId],
      wishes = source.field("ws").seq[WishId],
      gifts = source.field("gs").seq[PresentId],
      statistics = source.field("st").value[UserStatistics],
      optimisticLock = source.field("ol").value[Int](0)
    )
  }
}