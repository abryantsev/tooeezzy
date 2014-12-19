package com.tooe.core.migration

import com.tooe.core.db.graph.UserGraphActor
import com.tooe.core.db.graph.msg.GraphPutUser
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import com.tooe.core.migration.DictionaryIdMappingActor._
import com.tooe.core.migration.IdMappingDataActor.SaveIdMapping
import com.tooe.core.migration.api.{MigrationResponse, DefaultMigrationResult}
import com.tooe.core.migration.db.domain._
import com.tooe.core.usecase.UserPhoneDataActor.UserPhoneExist
import com.tooe.core.usecase.country.CountryDataActor.GetCountry
import com.tooe.core.usecase.maritalstatus.MaritalStatusDataActor
import com.tooe.core.usecase.region.RegionDataActor.GetRegion
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.usecase.{UpdateStatisticActor, UserPhoneDataActor}
import com.tooe.core.util.InfoMessageHelper
import java.util.Date
import org.bson.types.ObjectId
import scala.{Option, Some}
import scala.concurrent.Future
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.security.CredentialsDataActor
import com.toiserver.core.usecase.registration.message.XMPPRegistration
import com.tooe.core.application.Actors

object UserMigratorActor {
  final val Id = 'userMigratorActor

  case class LegacyPhone(code: String, number: String) {
    def upgrade = Phone(code, number, None)
  }
  case class LegacySettings(pagerights: Seq[Int], maprights: Seq[Int], showage: Boolean)
  case class LegacyMessageSettings(showtext: Option[Boolean], playaudio: Option[Boolean], sendemail: Option[LegacySendMail])
  case class LegacySendMail(email: String, events: Seq[String]) extends UnmarshallerEntity
  case class LegacyMedia(id: String, t: String, purpose: Option[String], f: Option[String], description: Option[String], descstyle: Option[String], desccolor: Option[String]) {
    private val mediaPurposeTransform: String => String = {
      case "background" => "bg"
      case otherwise => otherwise
    }
    def upgrade = UserMedia(url = MediaObject(MediaObjectId(id), Some(UrlType.http)), description = description, mediaType = t, purpose = purpose.map(mediaPurposeTransform), descriptionColor = desccolor, descriptionStyle = descstyle, videoFormat = f) //TODO migration rules
  }
  case class LegacyStar(starcategories: Seq[Int], presentmessage: Option[String], subscriberscount: Int, agentid: Option[Int])
  case class LegacyUser(legacyid: Int, name: String, lastname: String,
                        email: String, regionid: Int, gender: String,
                        birthday: Option[Date], maritalstatus: Option[MaritalStatusId], mainphone: Option[LegacyPhone],
                        phones: Option[Seq[LegacyPhone]],
                        education: Option[String], job: Option[String], aboutme: Option[String],
                        settings: LegacySettings, messagesettings: LegacyMessageSettings, pwd: String,
                        media: Seq[LegacyMedia], regdate: Date, star: Option[LegacyStar]) extends UnmarshallerEntity

}

class UserMigratorActor extends MigrationActor {

  import UserMigratorActor._

  def receive = {
    case lu: LegacyUser =>
      val future = for {
        (ua, cu) <- addressify(lu)
        us <- starify(lu)
        st <- settingify(lu)
        user = upgradeUser(lu)(ua, us, st, cu)
        isPhoneDup <- isFunctionalPhoneAlreadyExist(user)
        _ <- if (isPhoneDup) InfoMessageHelper.throwAppExceptionById("duplicate_functional_phone_number")
        else Future successful None
        _ <- userGraphActor ? new GraphPutUser(user.id)
        _ <- userDataActor ? UserDataActor.SaveUser(user)
        migrationResult <- userSaveIdMappingAndCredentials(user, lu)
      } yield {
        updateStatisticsAndSaveUrls(user)
        xmppAccountActor ! new XMPPRegistration(user.id.id.toString, user.id.id.toString)
        MigrationResponse(migrationResult)
      }
      future pipeTo sender
  }

  def userSaveIdMappingAndCredentials(user: User, lu: LegacyUser): Future[DefaultMigrationResult] = {
    val cred = Credentials(id = new ObjectId(), uid = user.id.id, userName = user.contact.email, passwordHash = "", //Sorry for that Ad-Hoc...
      legacyPasswordHash = Some(lu.pwd), facebookId = None, vkontakteId = None, twitterId = None, verificationTime = None, verificationKey = None)
    (credentialsDataActor ? CredentialsDataActor.Save(cred)).flatMap {
      cred =>
        (idMappingDataActor ? SaveIdMapping(IdMapping(new ObjectId(), MappingCollection.user, lu.legacyid, user.id.id))) map {
          case idm: IdMapping =>
            DefaultMigrationResult(idm.legacyId, idm.newId, "user_migrator")
        }
    }
  }
  def isFunctionalPhoneAlreadyExist(user: User): Future[Boolean] = {
    user.contact.phones.main map {
      phone =>
        (userPhoneDataActor ? UserPhoneExist(Option(phone.countryCode).getOrElse(""), phone.number)).mapTo[Boolean]
    } getOrElse Future(false)
  }

  def updateStatisticsAndSaveUrls(user: User) = {
    user.userMedia.foreach {
      m =>
        val mediaType = m.purpose match {
          case Some("bg") => UrlField.UserMediaBackground
          case _ => UrlField.UserMediaForeground
        }
        saveUrl(EntityType.user, user.id.id, m.url.url.id, mediaType)
    }
    updateStatisticActor ! UpdateStatisticActor.ChangeUsersCounter(user.contact.address.regionId, 1)
  }

  private def addressify(lu: LegacyUser): Future[(UserAddress, CurrencyId)] = {
    for {
      regionId <- (dictionaryIdMappingActor ? GetRegionId(lu.regionid)).mapTo[RegionId]
      region <- (regionDataActor ? GetRegion(regionId)).mapTo[Region]
      country <- (countryDataActor ? GetCountry(region.countryId)).mapTo[Country]
    } yield (UserAddress(region.countryId, region.id, region.name.getByLang.get, country.name.getByLang.get), country.currency.getOrElse(CurrencyId("RUR")))
  }
  private def starify(lu: LegacyUser): Future[Option[UserStar]] = {
    lu.star match {
      case None => Future successful None
      case Some(star) =>
        Future.sequence(star.starcategories.map(dictionaryIdMappingActor ? GetStarCategoryId(_))).mapTo[Seq[StarCategoryId]] flatMap {
          case xs =>
            val agentIdFuture: Future[Option[AdminUserId]] = star.agentid.map {
              legacyAgent =>
                lookupByLegacyId(legacyAgent, MappingCollection.admUser)
                  .map(idm => Some(AdminUserId(idm)))
            }.getOrElse(Future successful None)
            agentIdFuture.map(agentOpt => Some(UserStar(xs, star.presentmessage, agentOpt, star.subscriberscount)))
        }
    }
  }
  private def settingify(lu: LegacyUser): Future[UserSettings] = {
    import lu.messagesettings._
    import lu.settings._
    Future.sequence(pagerights ++ maprights map (dictionaryIdMappingActor ? GetUserGroup(_))).mapTo[Seq[String]] map {
      xs =>
        val (pr, mr) = xs.splitAt(pagerights.length)
        UserSettings(pr.filter(!_.isEmpty), mr.filter(!_.isEmpty), showage, UserMessageSetting(showtext, playaudio, sendemail
          .map(ls => UserSendEmailEvent(ls.email, ls.events))))
    }
  }

  private def upgradeUser(lu: LegacyUser)(ua: UserAddress, us: Option[UserStar], set: UserSettings, cu: CurrencyId): User = {
    lu match {
      case lu: LegacyUser =>
        import lu._
        User(UserId(), name, lastname, None, birthday, gender match {
          case "f" => Gender.Female
          case _ => Gender.Male
        }, maritalstatus, None, UserContact(ua,
          (for {
            phs <- Some(phones.map(_.map(_.upgrade)))
            mph <- Some(mainphone.map(_.upgrade))
          } yield UserPhones(phs, mph)).get, lu.email), //keep it safe!
          UserDetails(cu, regdate, education, job, aboutme), us, set,
          media.map(_.upgrade), Nil, Nil, Nil, Nil, UserStatistics())
    }
  }

  lazy val credentialsDataActor = lookup(CredentialsDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val maritalStatusDataActor = lookup(MaritalStatusDataActor.Id)
  lazy val dictionaryIdMappingActor = lookup(DictionaryIdMappingActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val userGraphActor = lookup(UserGraphActor.Id)
  lazy val userPhoneDataActor = lookup(UserPhoneDataActor.Id)
  lazy val xmppAccountActor = lookup(Actors.XMPPAccountActor)

}