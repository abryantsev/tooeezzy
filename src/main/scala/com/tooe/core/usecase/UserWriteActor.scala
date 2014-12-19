package com.tooe.core.usecase

import com.tooe.api.service._
import com.tooe.core.application.Actors
import com.tooe.core.db.graph.UserGraphActor
import com.tooe.core.db.graph.msg.{GraphPutUser, GraphPutUserAcknowledgement}
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import com.tooe.core.exceptions.{BadRequestException, ApplicationException}
import com.tooe.core.usecase.UploadMediaServerActor.SavePhoto
import com.tooe.core.usecase.UserPhoneDataActor.SaveUserPhone
import com.tooe.core.usecase.UserPhoneDataActor.UserPhoneExist
import com.tooe.core.usecase.session.CacheUserOnlineDataActor
import com.tooe.core.usecase.urls.{UrlsDataActor, UrlsWriteActor}
import com.tooe.core.usecase.user.{UpdatePhonesFields, UpdateFields, UserDataActor}
import com.tooe.core.util.{InfoMessageHelper, Lang}
import country.CountryDataActor
import region.RegionDataActor
import scala.concurrent.Future
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.usecase.present.PresentDataActor

object UserWriteActor {
  final val Id = Actors.UserWrite

  case class CreateNewUser(params: RegistrationParams, lang: Lang)
  case class CreateNewStar(params: StarRegistrationParams, adminUserId: AdminUserId, lang: Lang)
  case class UpdateUserProfile(userId: UserId, updateRequest: UserProfileUpdateRequest, lang: Lang)
  case class IncrementUserCounter(userId: UserId, delta: Int)
  case class SetUserMainMedia(userId: UserId, request: SetMainAvatar, lang: Lang)
  case class AddUserMedia(userId: UserId, request: AddUserMediaRequest, lang: Lang, replaceMainAvatar: Boolean)
  case class DeleteUserMedia(userId: UserId, request: DeleteUserMediaRequest)
}

class UserWriteActor extends AppActor with ExecutionContextProvider {

  val maxPhoneNumber = context.system.settings.config.getInt("constraints.user.phones.max-number")

  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val regionDataActor = lookup(RegionDataActor.Id)
  lazy val countryDataActor = lookup(Actors.CountryData)
  lazy val uploadMediaServerActor = lookup(UploadMediaServerActor.Id)
  lazy val userGraphActor = lookup(UserGraphActor.Id)
  lazy val userPhoneDataActor = lookup(UserPhoneDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val cacheUserOnlineDataActor = lookup(CacheUserOnlineDataActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val deleteMediaServerActor = lookup(DeleteMediaServerActor.Id)
  lazy val urlsDataActor = lookup(UrlsDataActor.Id)
  lazy val presentDataActor = lookup(PresentDataActor.Id)

  import UserWriteActor._

  def receive = {

    case CreateNewUser(params, lang) =>
      createUserFromParams(params)(lang) pipeTo sender

    case CreateNewStar(params, adminUserId, lang) =>
      createStarFromParams(params, adminUserId)(lang) pipeTo sender

    case UpdateUserProfile(userId, updateRequest, lang) =>
      implicit val lng = lang

      def updateUserMedia(userId: UserId, newUserMedia: UserProfileMediaUpdateRequest) =
        getUserMedia(userId).flatMap (user =>
          uploadToMediaServer(newUserMedia, userId).flatMap(url => addUserMediaByUrl(userId, url, lang, replaceMainAvatar = true))
        )

      val result = for {
        (region, country) <- loadRegionAndCountryFuture(updateRequest.regionId)
        _ <- updateRequest.media.map(newMedia => updateUserMedia(userId, newMedia)) getOrElse Future.successful(SuccessfulResponse)
        updatePhonesFields <- getNewUserPhones(userId, updateRequest)
        newPhoneSet = updatePhonesFields.all.toOption getOrElse Seq()
        _ <- checkMaxNumberOfUserPhones(newPhoneSet)
        _ <- updateUserPhonesCollection(userId, newPhoneSet)
      } yield {
        updateRequest.newOnlineStatus.map(os =>
          cacheUserOnlineDataActor ! CacheUserOnlineDataActor.UpdateOnlineStatus(userId, os)
        )
        userDataActor ! UserDataActor.UserUpdate(userId,
          UpdateFields(
            name = updateRequest.name,
            lastName = updateRequest.lastName,
            email = updateRequest.email,
            regionId = region.map(_.id),
            regionName = region.map(_.name.localized.getOrElse("")),
            countryId = country.map(_.id),
            countryName = country.map(_.name.localized.getOrElse("")),
            birthday = updateRequest.birthday,
            gender = updateRequest.gender,
            maritalStatus = updateRequest.maritalStatus,
            education = updateRequest.education,
            job = updateRequest.job,
            aboutMe = updateRequest.aboutMe,
            settings = updateRequest.settings,
            messageSettings = updateRequest.messageSettings,
            onlineStatus = updateRequest.onlineStatus,
            phones = updatePhonesFields
           )
          )
        SuccessfulResponse
      }
      result pipeTo sender

    case IncrementUserCounter(userId, delta) =>
      (userDataActor ? UserDataActor.GetUser(userId)).mapTo[User].map { user =>
        updateStatisticActor ! UpdateStatisticActor.ChangeUsersCounter(user.contact.address.regionId, 1)
      }

    case SetUserMainMedia(userId, request, lang) =>
      getUserMedia(userId).flatMap { user => {
        user.ifAvatarExists(request.media.url) match {
          case Right(media) => Future.successful(
            (user.findAvatarMedia.filterNot(_.url.url.id == media.url.url.id).map(_.copy(purpose = None)) ++
            user.videoMedia ++ user.backgroundMedia :+ media.copy(purpose = Some("main")), user)
          )
          case Left(_) => InfoMessageHelper.throwAppExceptionById("media_with_such_url_not_exists")(lang)
        }
      }}.flatMap{setUpdatedUserMediaWithOptimisticLocking(userId, self.ask(SetUserMainMedia(userId, request, lang)), lang)}pipeTo sender

    case AddUserMedia(userId, request, lang, replaceMainAvatarWhenLimitExceeded) =>
      import request._
      getUserMedia(userId).flatMap { user =>
        request.purpose match {
          case Some("background") =>
            addUrlsBackgroundUserMedia(userId, request.url)
            val (stillUserMedia, removeUserMedia) = user.userMedia.partition(_.purpose != Some("bg"))
            urlsDataActor ! UrlsDataActor.DeleteUrlsByEntityAndUrl(removeUserMedia.map(media => userId.id ->  media.url.url))
            Future.successful((stillUserMedia :+ newBackgroundUserMedia, user))
          case None =>  // adding avatar media
            if(mediaUrlAlreadyExists(user.userMedia))
              InfoMessageHelper.throwAppExceptionById("media_with_such_url_already_exists")(lang)
            else if(!replaceMainAvatarWhenLimitExceeded && user.isLimitOfAvatarMediaReached)
              InfoMessageHelper.throwAppExceptionById("overflow_max_user_avatar_count")(lang)
            else {
              addUrlsUserMedia(userId, request.url)

              val userMedia: Seq[UserMedia] =
                if (user.isLimitOfAvatarMediaReached) user.userMedia.filterNot(_.isMain) :+ newMainAvatarUserMedia
                else if (user.findAvatarMedia.size > 0) user.userMedia :+ newAvatarUserMedia
                else user.userMedia :+ newMainAvatarUserMedia

              Future successful (userMedia, user)
            }
          case _ => Future.failed(BadRequestException("Invalid state"))
        }
      }.flatMap{setUpdatedUserMediaWithOptimisticLocking(userId, self.ask(AddUserMedia(userId, request, lang, replaceMainAvatar = replaceMainAvatarWhenLimitExceeded)), lang)} pipeTo sender

    case DeleteUserMedia(userId, request) =>
      (for {
        user <- getUserMedia(userId)
        (removedUserMedia, changedUserMedia) = user.userMedia.partition(_.url.url.id == request.url)
      } yield {
        val (deleteImageInfo, deleteUrls) = removedUserMedia.map { media =>
          val imageType = media.purpose.map(_ => ImageType.banner).getOrElse(ImageType.avatar)
          ImageInfo(media.url.url.id, imageType, userId.id) -> (userId.id -> media.url.url)
        }.unzip
        deleteMediaServerActor ! DeleteMediaServerActor.DeletePhotoFile(deleteImageInfo)
        urlsDataActor ! UrlsDataActor.DeleteUrlsByEntityAndUrl(deleteUrls)
        userDataActor ! UserDataActor.SetUserMedia(userId, changedUserMedia, None)
        SuccessfulResponse
      }) pipeTo sender

  }

  def setUpdatedUserMediaWithOptimisticLocking(userId: UserId, retryFtr: => Future[Any], lang: Lang): PartialFunction[(Seq[UserMedia], UserMediaHolder), Future[SuccessfulResponse]] = {
    case (um, umh) => {
      setUpdatedUserMedia(userId, um, umh.optimisticLock).flatMap {
        case UpdateResult.NotFound => retryFtr.mapTo[SuccessfulResponse]
        case _ => Future.successful(SuccessfulResponse)
      }
    }
  }

  def getNewUserPhones(userId: UserId, updateRequest: UserProfileUpdateRequest)(implicit lang: Lang): Future[UpdatePhonesFields] = {
    import Unsetable._
    (updateRequest.mainPhone, updateRequest.additionalPhones) match {
      case (Unset, Unset) =>
        Future successful UpdatePhonesFields(Unset, Unset)

      case (Unset, Skip) =>
        getUserPhones(userId) map { userPhones =>
          val allButMain = userPhones.additionalShort
          val newMain = allButMain.headOption map Update.apply getOrElse Unset
          UpdatePhonesFields(newMain, Update(allButMain))
        }

      case (Update(mp), Skip) =>
        for {
          userPhones <- getUserPhones(userId)
          newPhoneSet = userPhones.allShort.toSet + mp
        } yield UpdatePhonesFields(updateRequest.mainPhone, Update(newPhoneSet.toSeq))

      case (Skip, Unset) =>
        for {
          userPhones <- getUserPhones(userId)
          newMainPhone = userPhones.mainShort map (x => Update(Seq(x))) getOrElse Unset
        } yield UpdatePhonesFields(Skip, newMainPhone)

      case _ =>
        val newPhoneSet = updateRequest.additionalPhones.toSeq.flatten ++ updateRequest.mainPhone.toSeq
        val allPhones: Unsetable[Seq[PhoneShort]] = if (newPhoneSet.nonEmpty) Update(newPhoneSet) else Skip
        Future successful UpdatePhonesFields(updateRequest.mainPhone, allPhones)
    }
  }

  def checkMaxNumberOfUserPhones(newPhoneSet: Seq[PhoneShort])(implicit lang: Lang): Future[_] =
    if (newPhoneSet.size <= maxPhoneNumber) Future successful ()
    else infoMessageActor ? InfoMessageActor.GetFailure("max_number_user_phones_has_been_achieved", lang)

  def updateUserPhonesCollection(userId: UserId, newPhoneSet: Seq[PhoneShort])(implicit lang: Lang): Future[_] =
    checkPhonesForUniqueness(userId, newPhoneSet.toSeq) map { _ =>
      updateUserPhones(userId, newPhoneSet)
    }

  def checkPhonesForUniqueness(userId: UserId, newPhoneSet: Seq[PhoneShort])(implicit lang: Lang): Future[_] =
    findNonUniqueUserPhones(userId, newPhoneSet) flatMap { nonUniquePhones =>
      if (nonUniquePhones.isEmpty) Future successful ()
      else (infoMessageActor ? InfoMessageActor.GetMessage("non_unique_user_phone_numbers", lang)).mapTo[String] flatMap { msg =>
        Future failed ApplicationException(message = msg + nonUniquePhones.mkString(", "))
      }
    }

  def updateUserPhones(userId: UserId, phones: Seq[PhoneShort]): Future[_] =
    userPhoneDataActor ? UserPhoneDataActor.UpdateUserPhones(userId, phones)

  def findNonUniqueUserPhones(userId: UserId, newPhoneSet: Seq[PhoneShort]): Future[Seq[PhoneShort]] =
    (userPhoneDataActor ? UserPhoneDataActor.FindNonUniquePhones(userId, newPhoneSet)).mapTo[Seq[PhoneShort]]

  def getUserPhones(userId: UserId): Future[UserPhones] =
    (userDataActor ? UserDataActor.GetUserPhones(userId)).mapTo[UserPhones]
  
  def addUserMediaByUrl(userId: UserId, url: String, lang: Lang, replaceMainAvatar: Boolean): Future[SuccessfulResponse] = {
    self.ask(AddUserMedia(userId, AddUserMediaRequest(url, None, None, None, None), lang, replaceMainAvatar = replaceMainAvatar)).mapTo[SuccessfulResponse]
  }

  def uploadToMediaServer(newUserMedia: UserProfileMediaUpdateRequest, userId: UserId): Future[String] = {
    (uploadMediaServerActor ? SavePhoto(ImageInfo(newUserMedia.value, ImageType.avatar, userId.id))).mapTo[String]
  }

  def getUserMedia(userId: UserId): Future[UserMediaHolder] = {
    userDataActor.ask(UserDataActor.GetUserMedia(userId)).mapTo[UserMediaHolder]
  }

  def setUpdatedUserMedia(userId: UserId, newMedia: Seq[UserMedia], optimisticLock: Int) = {
    userDataActor.ask(UserDataActor.SetUserMedia(userId, newMedia, Some(optimisticLock))).mapTo[UpdateResult]
  }

  def addUrlsUserMedia(userId: UserId, url: String) {
    urlsWriteActor ! UrlsWriteActor.AddUserMediaUrl(userId, MediaObjectId(url))
  }

  def addUrlsBackgroundUserMedia(userId: UserId, url: String) {
    urlsWriteActor ! UrlsWriteActor.AddUserBackgroundUrl(userId, MediaObjectId(url))
  }

  def getInvalidMainMedia(lang: Lang): Future[String] = {
    (infoMessageActor ? InfoMessageActor.GetMessage("invalid_main_avatar", lang)).mapTo[String]
  }

  def getCountry(id: CountryId): Future[Country] = (countryDataActor ? CountryDataActor.GetCountry(id)).mapTo[Country]

  def getRegion(id: RegionId): Future[Region] = (regionDataActor ? RegionDataActor.GetRegion(id)).mapTo[Region]

  def loadRegionAndCountryFuture(regionIdOpt: Option[RegionId]) =
    (regionIdOpt.map { regionId =>
      getRegion(regionId) flatMap { region =>
        getCountry(region.countryId) map { country =>
          (Some(region), Some(country))
        }
      }
    } getOrElse Future.successful((None, None))).mapTo[(Option[Region], Option[Country])]

  def createStarUser(userId: UserId, region: Region, country: Country, adminUserId: AdminUserId, params: StarRegistrationParams)(implicit lang: Lang): User = {
    User(
      id = userId,
      name = params.name,
      lastName = params.lastName,
      birthday = None,
      userMedia = Seq(), //params.media.toSeq.map(url => UserMedia.mainFromUrl(url)),    // TODO mainUrl or avatarUrl
      gender = params.gender,
      contact = UserContact.forRegistration(country, region, Some(params.registrationPhone), params.email),
      details = UserDetails.forRegistration(country),
      settings = UserSettings.forRegistration,
      statistics = UserStatistics(),
      star = Some(params.star(adminUserId))
    )
  }
  def createUser(userId: UserId, region: Region, country: Country, params: RegistrationParams)(implicit lang: Lang): User = {
    User(
      id = userId,
      name = params.name,
      lastName = params.lastName,
      birthday = Option(params.birthday),
      gender = params.gender,
      contact = UserContact.forRegistration(country, region, params.registrationPhone, params.email),
      details = UserDetails.forRegistration(country),
      settings = UserSettings.forRegistration,
      statistics = UserStatistics()
    )
  }

  def saveUser(user: User): Future[User] = (userDataActor ? UserDataActor.SaveUser(user)).mapTo[User]

  def putUserInGraph(userId: UserId): Future[GraphPutUserAcknowledgement] =
    (userGraphActor ? new GraphPutUser(userId)).mapTo[GraphPutUserAcknowledgement]

  def isFunctionalPhoneAlreadyExists(phone: Option[String], countryCode: Option[String])(implicit lang: Lang): Future[Boolean] = phone map { phone =>
    (userPhoneDataActor ? UserPhoneExist(countryCode.getOrElse(""), phone)).mapTo[Boolean]
  } getOrElse (Future successful false).flatMap(p =>
    if (p) InfoMessageHelper.throwAppExceptionById("duplicate_functional_phone_number")(lang) else Future.successful(p))

  def validateEmailUniqueness(email: String): Future[_] =
    userDataActor.ask(UserDataActor.FindUserIdByEmail(email)).mapTo[Option[UserId]] flatMap {
      case Some(_) => Future failed ApplicationException(message = "User with this e-mail already exists.")
      case _       => Future successful ()
    }

  def createUserFromParams(params: RegistrationParams, newUserId:UserId = UserId())(implicit lang: Lang): Future[User] =
    for {
      _ <- validateEmailUniqueness(params.email)
      _ <- isFunctionalPhoneAlreadyExists(params.phone, params.countryCode)
      region <- getRegion(params.regionId)
      country <- getCountry(region.countryId)
      _ <- putUserInGraph(newUserId) zip validateEmailUniqueness(params.email) zip isFunctionalPhoneAlreadyExists(params.phone, params.countryCode)
      newUser = createUser(newUserId, region, country, params)
      user <- saveUser(newUser)
    } yield {
      assignUserPresents(params.email, params.phoneShort)(user.id)
      saveUserPhone(params.registrationPhone, user)
      user
    }

  def assignUserPresents(email: String, phone: Option[PhoneShort])(userId: UserId): Unit = {
    presentDataActor ! PresentDataActor.AssignUserPresents(phone, Some(email), userId)
  }

  def createStarFromParams(params: StarRegistrationParams, adminUserId: AdminUserId, newUserId:UserId = UserId())(implicit lang: Lang): Future[User] =
    for {
      region <- getRegion(params.regionId)
      country <- getCountry(region.countryId)
      _ <- putUserInGraph(newUserId) zip validateEmailUniqueness(params.email) zip isFunctionalPhoneAlreadyExists(Some(params.phone), Some(params.countryCode))
      newUser = createStarUser(newUserId, region, country, adminUserId, params)
      user <- saveUser(newUser)
    } yield {
      saveUserPhone(Some(params.registrationPhone), user)
      user
    }

  def saveUserPhone(phone: Option[Phone], user: User): Unit = {
    userPhoneDataActor ! SaveUserPhone(user.id, phone.getOrElse(Phone("", "")))
  }
}