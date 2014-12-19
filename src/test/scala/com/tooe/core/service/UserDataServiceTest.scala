package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.HashHelper
import com.tooe.core.domain._
import com.tooe.api.service._
import com.tooe.core.db.mongo.query.UpdateResult
import java.util.Date
import com.tooe.core.usecase._
import com.tooe.core.db.mongo.domain.UserStatisticsNew
import com.tooe.core.usecase.user.{UpdatePhonesFields, UserMessageSettingUpdateItem, UpdateUserStatistic, UpdateFields}
import com.tooe.api.service.SetMainAvatar
import com.tooe.core.db.mongo.domain.UserStatistics
import com.tooe.api.service.SearchStarsRequest
import com.tooe.core.db.mongo.domain.UserSendEmailEvent

class UserDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: UserDataService = _

  lazy val entities = new MongoDaoHelper("user")

  @Test
  def saveAndReadAndDelete {
    val entity = defaultUser
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
    service.delete(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def representation {
    val user = new UserFixture().user
    service.save(user)
    val repr = entities.findOne(user.id.id)
    println("repr="+repr)
    jsonAssert(repr)(s"""{
      "_id" : ${user.id.id.mongoRepr} ,
      "n" : "${user.name}" ,
      "ln" : "last-name" ,
      "sn" : "second-name" ,
      "ns" : [ "${user.name.toLowerCase} last-name second-name" , "${user.name.toLowerCase} second-name last-name" , "last-name ${user.name.toLowerCase} second-name" , "last-name second-name ${user.name.toLowerCase}" , "second-name ${user.name.toLowerCase} last-name" , "second-name last-name ${user.name.toLowerCase}"],
      "bd" : ${user.birthday.getOrElse(new Date).mongoRepr} ,
      "g" : "f" ,
      "fs" : "marital-status" ,
      "c" : {
        "a" : {
          "rid" : ${user.contact.address.regionId.id.mongoRepr} ,
          "r" : "region-name" ,
          "co" : "country-name",
          "cid" : "${user.contact.address.countryId.id}"
        } ,
        "p" : {
          "f" : [ { "c" : "7" , "n" : "1111111111"}] ,
          "mf" : { "c" : "7" , "n" : "5555555555"}
        } ,
        "e" : "user@server.domain"
      } ,
      "d" : {
        "cu" : "${user.details.defaultCurrency.id}",
        "rt" : ${user.details.registrationTime.mongoRepr} ,
        "e" : "education" ,
        "job" : "job" ,
        "am" : "about"
      } ,
      "star" : {
        "sc" : [ "star-category-id"] ,
        "pm" : "present-answer-template",
        "suc" : ${user.star.map(_.subscribersCount).getOrElse(0L)},
        "aid" : ${user.star.flatMap(_.agentId).getOrElse(AdminUserId()).id.mongoRepr}
      } ,
      "s" : {
        "pr" : [ "some-page-right"] ,
        "mr" : [ "some-map-right"] ,
        "sa" : false ,
        "ms" : {
          "st" : true ,
          "a" : true ,
          "se" : {
            "e" : "some-email@whatever.exist" ,
            "ev" : [ "some-event"]
          }
        }
      } ,
      "um" : [ { "u" : { "mu": "url", "t": "s3" } , "d" : "description" , "t" : "mediaType" , "p" : "purpose" , "f" : "videoFormat" , "ds" : "descriptionStyle" , "dc" : "descriptionColor"}] ,
      "pa" : [ ${user.photoAlbums.head.id.mongoRepr} ] ,
      "lp" : [ ${user.lastPhotos.head.id.mongoRepr} ] ,
      "ws" : [ ${user.wishes.head.id.mongoRepr} ] ,
      "gs" : [ ${user.gifts.head.id.mongoRepr}],
      "st" : {
            "prc" : 0,
            "cc" : 0,
            "spc" : 0,
            "scc" : 0,
            "fc" : 0,
            "frc" : 0,
            "wc" : 0,
            "fwc" : 0,
            "flc" : 0,
            "ssc" : 0,
            "lsc" : 0,
            "pc" : 0,
            "ec" : 0,
            "new" : {
              "prc" : 0,
              "ec" : 0,
            }
          },
      "ol" : 0
    }""")
  }

  @Test
  def getUserOnline {
    val user = defaultUser.copy(onlineStatus = Some(OnlineStatusId.Busy))
    service.save(user)
    service.getUserOnline(user.id).onlineStatus === Some(OnlineStatusId.Busy)
  }

  @Test
  def setUserEventStatistic {
    val user = defaultUser.copy(statistics = UserStatistics(eventsCount = 1))
    service.save(user)
    service.setUserStatistic(user.id, UpdateUserStatistic(userEvents = Some(0)))
    service.findOne(user.id).map(_.statistics.eventsCount) === Some(0)

  }

  @Test
  def changeUserEvents {
    val user = defaultUser.copy(statistics = UserStatistics(eventsCount = 3, newStatistic = UserStatisticsNew(eventsCount = 2)))
    service.save(user)
    service.changeUserStatistic(user.id, UpdateUserStatistic(userEvents = Some(2), newUserEvents = Some(-2)))
    val changedUser = service.findOne(user.id)
    changedUser.map(_.statistics.eventsCount) === Some(5)
    changedUser.map(_.statistics.newStatistic.eventsCount) === Some(0)
  }

  @Test
  def changeUsersEvents {
    val user1 = defaultUser.copy(statistics = UserStatistics(newStatistic = UserStatisticsNew(eventsCount = 2)))
    val user2 = defaultUser.copy(statistics = UserStatistics(newStatistic = UserStatisticsNew(eventsCount = 1)))
    service.save(user1)
    service.save(user2)
    val userIds = Set(user1, user2).map(_.id)
    val delta = 1
    service.changeUsersStatistics(userIds, UpdateUserStatistic(newUserEvents = Some(delta)))
    val changedUsers = service.findUsersByUserIds(userIds.toSeq)
    changedUsers.find(_.id == user1.id).map(_.statistics.newStatistic.eventsCount) === Some(3)
    changedUsers.find(_.id == user2.id).map(_.statistics.newStatistic.eventsCount) === Some(2)
  }

  @Test
  def findByEmail {
    val email = HashHelper.uuid
    val u = defaultUser
    service.save(u)
    service.findUserIdByEmail(email) must (beEmpty)

    val user = u.copy(contact = u.contact.copy(email = email))
    service.save(user)
    service.findUserIdByEmail(email) === Some(user.id)
  }

  @Test
  def findByPhone {
    val code, number = HashHelper.uuid
    val userPhones = UserPhones(
      all = Some(Seq(Phone("some-code", "some-number"), Phone(code, number))),
      main = None
    )
    val user = new UserFixture(userPhones = userPhones).user

    service.findUserIdByPhone(code = code, number = number) === None
    service.save(user)
    service.findUserIdByPhone(code = code, number = number) === Some(user.id)
  }

  @Test
  def findUsersByUserIds {

    val userIds = 1 to 5 map ( i => UserId(ObjectId()))
    val users = userIds.map{ uid => service.save(defaultUser.copy(id = uid)) }

    service.findUsersByUserIds(userIds) === users
  }

  @Test
  def addGift {
    val u = defaultUser.copy(gifts = Nil)
    val p1, p2 = PresentId()
    service.addGift(u.id, p1) === UpdateResult.NotFound
    service.save(u)
    service.addGift(u.id, p1) === UpdateResult.Updated
    service.addGift(u.id, p2) === UpdateResult.Updated
    service.findOne(u.id).get.gifts === Seq(p1, p2)
  }

  @Test
  def findAbsentIds {
    val user = new UserFixture().user
    val absentIds = 1 to 10 map (_ => UserId())
    service.save(user)
    service.findAbsentIds(absentIds :+ user.id).toSet === absentIds.toSet
  }

  @Test
  def getUserMedia {
    val user = defaultUser
    service.save(user)
    val userMedia = service.getUserMedia(user.id)
    userMedia.birthday === None
    userMedia.contact === null
    userMedia.details === null
    userMedia.gender === null
    userMedia.gifts === List()
    userMedia.maritalStatusId === None
    userMedia.name === null
    userMedia.lastName === null
    userMedia.photoAlbums === List()
    userMedia.secondName === None
    userMedia.settings === null
    userMedia.star === None
    userMedia.statistics === null
    userMedia.wishes === List()
    userMedia.lastPhotos === List()
    userMedia.userMedia === user.userMedia
  }

  @Test
  def updateUser {
    import com.tooe.core.util.SomeWrapper._

    val user = defaultUser
    service.save(user)
    val updateUserPhones = UpdatePhonesFields(Unsetable.Update(PhoneShort("+1", "987654321")), Unsetable.Update(Seq(PhoneShort("+1", "987654321"))))
    val updateParams = UpdateFields(name = "new name",
                                    lastName = "new lastname",
                                    email = "new@email",
                                    regionId = RegionId(),
                                    countryId = CountryId("country id"),
                                    regionName = "new region name",
                                    countryName = "new country name",
                                    birthday = Unsetable.Update(new Date),
                                    gender = Gender.Male,
                                    maritalStatus = Unsetable.Update(MaritalStatusId("single")),
                                    education = Unsetable.Update("new education"),
                                    job = Unsetable.Update("new job"),
                                    aboutMe = Unsetable.Update("new aboutme"),
                                    settings = UserSettingsProfileItem(pageRights = Seq("pageRights"), mapRights = Seq("mapRights")),
                                    messageSettings = UserMessageSettingUpdateItem(
                                      showText = Unsetable.Update(true),
                                      playAudio = Unsetable.Update(true),
                                      sendEmail = Unsetable.Update(UserSendEmailEvent("new email", Seq("new events")))
                                    ),
                                    onlineStatus = Unsetable.Update(OnlineStatusId.ReadyForChat),
                                    phones = updateUserPhones
                                    )

    service.updateUser(user.id, updateParams)

    val updatedUser = service.findOne(user.id).get

    updatedUser.name === updateParams.name.get
    updatedUser.lastName === updateParams.lastName.get
    updatedUser.contact.email === updateParams.email.get
    updatedUser.contact.address.regionId === updateParams.regionId.get
    updatedUser.contact.address.countryId === updateParams.countryId.get
    updatedUser.contact.address.regionName === updateParams.regionName.get
    updatedUser.contact.address.country === updateParams.countryName.get
    updatedUser.birthday.get === updateParams.birthday.get
    updatedUser.gender === updateParams.gender.get
    updatedUser.contact.phones.main.map(p => PhoneShort(p.countryCode, p.number)).get === updateParams.phones.main.get
    updatedUser.contact.phones.all.map(ps => ps.map( p => PhoneShort(p.countryCode, p.number))).get === updateParams.phones.all.get
    updatedUser.userMedia === user.userMedia
    updatedUser.maritalStatusId.get === updateParams.maritalStatus.get
    updatedUser.details.education.get === updateParams.education.get
    updatedUser.details.job.get === updateParams.job.get
    updatedUser.details.aboutMe.get === updateParams.aboutMe.get
    updatedUser.settings.pageRights === updateParams.settings.get.pageRights
    updatedUser.settings.mapRights === updateParams.settings.get.mapRights
    updatedUser.settings.messageSettings.showMessageText.get === updateParams.messageSettings.get.showText.get
    updatedUser.settings.messageSettings.soundsEnabled.get === updateParams.messageSettings.get.playAudio.get
    updatedUser.settings.messageSettings.sendEmailEvent.get === updateParams.messageSettings.get.sendEmail.get
    updatedUser.onlineStatus.get === updateParams.onlineStatus.get

  }

  @Test
  def findUserPhones {
    val user = new UserFixture().user
    service.findUserPhones(user.id) === None

    service.save(user)
    service.findUserPhones(user.id).get === user.contact.phones
  }

  @Test
  def findUserByCountry {
    val user = new UserFixture().user.copy(star = None)
    service.save(user)

    val request = SearchUsersRequest(name = Some(user.name), country = Some(user.contact.address.countryId), None, None, None, None, OffsetLimit())
    val usersInCountry = service.searchUsers(request)
    usersInCountry must contain(user)
  }

  @Test
  def findUserByRegion {
    val user = new UserFixture().user.copy(star = None)
    service.save(user)

    val request = SearchUsersRequest(name = Some(user.name),
                                     country = Some(CountryId("country-id")),
                                     region = Some(user.contact.address.regionId),
                                     gender = None,
                                     maritalstatus = None,
                                     responseOptions = None,
                                     offsetLimit = OffsetLimit())
    val usersInRegion = service.searchUsers(request)
    usersInRegion must contain(user)

    val requestWithHalfNameInUpperCase = request.copy(name = Some(request.name.get.take(request.name.get.length / 2).toUpperCase))
    val usersInRegionByHalfName = service.searchUsers(requestWithHalfNameInUpperCase)
    usersInRegionByHalfName must contain(user)
  }

  @Test
  def searchUsersCount {
    val user = new UserFixture().user.copy(star = None)
    service.save(user)

    val request = SearchUsersRequest(name = Some(user.name), None, None, None, None, None, OffsetLimit())
    service.searchUsersCount(request) === 1

    service.searchUsersCount(request.copy(name = Some("incorrect name"))) === 0

  }

  @Test
  def getUserStatistic {
    val statistic = UserStatistics(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 0, 0, 0, UserStatisticsNew(10, 11))
    val user = new UserFixture().user.copy(statistics = statistic)
    service.save(user)

    val userStatistic = service.getUserStatistics(user.id)

    userStatistic === statistic
  }

  @Test
  def findStars {
    val user = new UserFixture().user
    service.save(user)

    val request = SearchStarsRequest(name = Some(user.name), country = Some(user.contact.address.countryId), None, None, None)
    val starsInCountry = service.searchStars(request, OffsetLimit())
    starsInCountry must contain(user)

    val requestWithHalfName = request.copy(name = Some(user.name.take(user.name.length / 2).toUpperCase))
    val starsFoundByHalfName = service.searchStars(requestWithHalfName, OffsetLimit())
    starsFoundByHalfName must contain(user)
  }

  @Test
  def searchStarsCount {
    val user = new UserFixture().user
    service.save(user)

    val request = SearchStarsRequest(name = Some(user.name), country = Some(user.contact.address.countryId), None, None, None)
    service.searchStarsCount(request) === 1

    service.searchStarsCount(request.copy(name = Some("incorrect name"))) === 0
  }

  @Test
  def addPhotoAlbum {
    val user = new UserFixture().user.copy(photoAlbums = Nil)
    service.save(user)

    val photoAlbumId = PhotoAlbumId()
    service.addPhotoAlbum(user.id, photoAlbumId)

    val user1 = service.findOne(user.id).get
    user1.photoAlbums === Seq(photoAlbumId)
  }

  @Test
  def removePhotoAlbum {
    val photoAlbumId = PhotoAlbumId()
    val user = new UserFixture().user.copy(photoAlbums = Seq(photoAlbumId))
    service.save(user)

    service.removePhotoAlbum(user.id, photoAlbumId)

    val user1 = service.findOne(user.id).get
    user1.photoAlbums === Nil
  }

  @Test
  def addWish {
    val user = new UserFixture().user.copy(wishes = Nil)
    service.save(user)

    val wishId = WishId()
    service.addWish(user.id, wishId)

    val user1 = service.findOne(user.id).get
    user1.wishes === Seq(wishId)
  }

  @Test
  def removeWish {
    val wishId = WishId()
    val user = new UserFixture().user.copy(wishes = Seq(wishId))
    service.save(user)

    service.removeWish(user.id, wishId)

    val user1 = service.findOne(user.id).get
    user1.wishes === Nil
  }

  @Test
  def addPhotoToUser {
    val user = defaultUser
    service.save(user)

    val photoId = PhotoId()
    service.addPhotoToUser(user.id, photoId)

    service.findOne(user.id).get.lastPhotos must haveTheSameElementsAs(photoId +: user.lastPhotos)

  }

  @Test
  def updateUserLastPhotos {
    val user = defaultUser.copy(lastPhotos = Seq(PhotoId(), PhotoId(), PhotoId()))
    service.save(user)

    val newPhotoIds = Seq(PhotoId(), PhotoId(), PhotoId())
    service.updateUserLastPhotos(user.id, newPhotoIds)

    val userPhotos = service.findOne(user.id).get.lastPhotos
    userPhotos must haveSize(newPhotoIds.size)
    userPhotos must haveTheSameElementsAs(newPhotoIds)
  }

  //TODO extend search field when they will implement
  @Test
  def findAndFilterUsersBy {
    val user = defaultUser
    service.save(user)

    val requestByName = SearchAmongOwnFriendsRequest(Some(user.name), Some(user.contact.address.country), None, None)
    val result1 = service.findAndFilterUsersBy(Seq(user.id), requestByName, OffsetLimit())
    result1 must contain(user)

    val requestByLastName = SearchAmongOwnFriendsRequest(Some(user.lastName), Some(user.contact.address.country), None, None)
    val result2 = service.findAndFilterUsersBy(Seq(user.id), requestByLastName, OffsetLimit())
    result2 must contain(user)

  }

  @Test
  def countFindAndFilterUsers {
    val user = defaultUser
    service.save(user)

    val request = SearchAmongOwnFriendsRequest(Some(user.name), Some(user.contact.address.country), None, None)
    service.countFindAndFilterUsers(Seq(user.id), request) === 1L

  }

  @Test
  def searchUserFriends {
    val user = defaultUser
    service.save(user)

    val request = SearchUserFriendsRequest(Some(user.name), None)
    val searchUsers = service.searchUserFriends(Seq(user.id), request, OffsetLimit())

    searchUsers must haveSize(1)
    searchUsers must contain(user)
  }

  @Test
  def countSearchUserFriends {
    val user = defaultUser
    service.save(user)

    val request = SearchUserFriendsRequest(Some(user.name), None)
    service.countSearchUserFriends(Seq(user.id), request) === 1L

  }

  @Test
  def updateUserMedia {
    val entity = new UserFixture().user
    service.save(entity)

    val userMediaHolder = service.getUserMedia(entity.id)
    val userMediaHolder2 = service.getUserMedia(entity.id)

    val userNewMedia = Seq(UserMedia(MediaObject(MediaObjectId(HashHelper.str("new_url")))))
    val userNewMedia2 = userMediaHolder2.userMedia :+ UserMedia(MediaObject(MediaObjectId(HashHelper.str("new_url_2"))))

    service.updateUserMedia(entity.id, userNewMedia, Some(userMediaHolder.optimisticLock))

    service.updateUserMedia(entity.id, userNewMedia2, Some(userMediaHolder2.optimisticLock))
    val user = service.findOne(entity.id)

    user.map(_.userMedia) === Some(userNewMedia)
    user.map(_.userMedia) !== Some(entity.userMedia)
    user.map(_.userMedia) !== Some(userNewMedia2)
    user.map(_.userMedia) === Some(userNewMedia)
  }

  @Test
  def updateMediaStorageToS3 {
    val media1 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val media2 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val userMedia1 = new UserMediaFixture().media.copy(url = media1)
    val userMedia2 = new UserMediaFixture().media.copy(url = media2)
    val user = new UserFixture().user.copy(userMedia = Seq(userMedia1, userMedia2))
    service.save(user)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(user.id, media2.url, expectedMedia.url)

    service.findOne(user.id).map(_.userMedia.map(_.url)) === Some(Seq(media1, expectedMedia))
  }

  @Test
  def updateMediaStorageToCDN {
    val userMedia1 = new UserMediaFixture().media.copy(url = MediaObject(MediaObjectId("url1"), Some(UrlType.s3)))
    val userMedia2 = new UserMediaFixture().media.copy(url = MediaObject(MediaObjectId("url2"), Some(UrlType.s3)))
    val user = new UserFixture().user.copy(userMedia = Seq(userMedia1, userMedia2))
    service.save(user)

    service.updateMediaStorageToCDN(user.id, userMedia2.url.url)

    service.findOne(user.id).map(_.userMedia) === Some(Seq(userMedia1, userMedia2.copy(url = MediaObject(MediaObjectId("url2"), None))))
  }

  @Test
  def findTopStars {

    val stars = service.findTopStars

    stars.size must beLessThanOrEqualTo(20)

  }

  def defaultUser = new UserFixture().user
}