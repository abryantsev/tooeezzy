package com.tooe.core.db.mongo.domain

import com.tooe.core.domain._
import java.util.{UUID, Date}

class UserFixture(userPhones: UserPhones = new UserPhonesFixture().userPhones)  {

  import com.tooe.core.util.SomeWrapper._

  val userContact = UserContact(
    address = UserAddress(
      countryId = CountryId("country-id:" + UUID.randomUUID.toString),
      regionId = RegionId(),
      regionName = "region-name",
      country = "country-name"
    ),
    phones = userPhones,
    email = "user@server.domain"
  )

  val userDetails = UserDetails(
    defaultCurrency = CurrencyId("RUR"),
    registrationTime = new Date(1374673989856L),
    education = "education",
    job = "job",
    aboutMe = "about"
  )

  val userStar = UserStar(
    starCategoryIds = Seq(StarCategoryId("star-category-id")),
    presentMessage = "present-answer-template",
    subscribersCount = 0,
    agentId = AdminUserId()
  )

  val sendEmailEvent = UserSendEmailEvent(
    email = "some-email@whatever.exist",
    events = Seq("some-event")
  )

  val userMessageSetting = UserMessageSetting(
    showMessageText = true,
    soundsEnabled = true,
    sendEmailEvent = sendEmailEvent
  )

  val userSettings = UserSettings(
    pageRights = Seq("some-page-right"),
    mapRights = Seq("some-map-right"),
    showAge = false,
    messageSettings = userMessageSetting
  )

  val userMedia = Seq(new UserMediaFixture().media)

  val userPhotoAlbums = Seq(PhotoAlbumId())

  val userLastSixPhotoIds = Seq(PhotoId())

  val userWishes = Seq(WishId())

  val userGifts = Seq(PresentId())

  val maritalStatusId = MaritalStatusId("marital-status")

  val userStatistics = UserStatistics()

  val user = User(
    id = UserId(),
    name = "name:" + UUID.randomUUID.toString,
    lastName = "last-name",
    secondName = "second-name",
    birthday = new Date(1374673934856L),
    gender = Gender.Female,
    maritalStatusId = maritalStatusId,
    onlineStatus = None,
    contact = userContact,
    details = userDetails,
    star = userStar,
    settings = userSettings,
    userMedia = userMedia,
    photoAlbums = userPhotoAlbums,
    lastPhotos = userLastSixPhotoIds,
    wishes = userWishes,
    gifts = userGifts,
    statistics = userStatistics
  )
}