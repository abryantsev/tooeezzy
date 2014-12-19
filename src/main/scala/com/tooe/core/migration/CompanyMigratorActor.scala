package com.tooe.core.migration

import com.tooe.core.usecase.company.{PreModerationCompanyDataActor, CompanyDataActor}
import java.util.Date
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.MappingCollection
import scala.concurrent.Future
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.admin_user.{AdminCredentialsDataActor, AdminUserDataActor}
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.AdminUserId
import com.tooe.core.db.mongo.domain.Phone
import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.db.mongo.domain.CompanyInformation
import com.tooe.core.db.mongo.domain.CompanyManagement
import scala.Some
import com.tooe.core.db.mongo.domain.AdminCredentials
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.db.mongo.domain.CompanyPayment
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.db.mongo.domain.CompanyContract
import com.tooe.core.db.mongo.domain.CompanyMedia
import com.tooe.core.migration.db.domain.IdMapping
import com.tooe.core.db.mongo.domain.Company
import com.tooe.core.db.mongo.domain.CompanyContact
import com.tooe.core.db.mongo.domain

object CompanyMigratorActor {
  final val Id = 'companyMigrator

  case class LegacyCompanyContact(legaladdress: String, address: String, countrycode: String, phone: String)
  case class LegacyCompanyMainUser(email: String, pwd: String, name: String, lastname: String)
  case class LegacyCompany(legacyid: Int, agentid: Int, contractnumber: String,
                           contractdate: Date, companyname: String, contact: LegacyCompanyContact,
                           legalinfo: LegacyCompanyLegalInfo, subject: Option[String], licence: Option[String],
                           media: Option[String], mainuser: LegacyCompanyMainUser) extends UnmarshallerEntity
  case class LegacyCompanyLegalInfo(office: String, manager: String, accountant: String,
                                    description: String, url: Option[String], structure: String,
                                    ogrn: Option[String], registrationdate: Option[Date], registrator: Option[String],
                                    taxnumber: String, kpp: Option[String])
}

class CompanyMigratorActor extends MigrationActor {
  import CompanyMigratorActor._

  def receive = {
    case lc: LegacyCompany =>
      (for {
        (cmp, cmpMod) <- legacyCompanyToCompany(lc)
        _ <- saveModeraion(cmpMod)
        _ <- saveCompany(cmp, lc.legacyid)
      } yield {
        MigrationResponse(DefaultMigrationResult(lc.legacyid, cmp.id.id, "company_migrator"))
      }) pipeTo sender
  }

  def legacyCompanyToCompany(lc: LegacyCompany): Future[(Company, PreModerationCompany)] = {
    val companyId = CompanyId()
    for {
      newAgentId <- lookupByLegacyId(lc.agentid, MappingCollection.admUser).map(AdminUserId)
      (user, creds) = convertLegacyMainUser(lc.mainuser, companyId)
      _ <- saveAdminUser(user)
      _ <- saveAdminCredentials(creds)
    } yield {
      val cmp = Company(
        id = companyId,
        mainUserId = user.id,
        agentId = newAgentId,
        shortName = Some(lc.companyname),
        registrationDate = lc.legalinfo.registrationdate.getOrElse(new Date()),
        name = lc.legalinfo.structure,
        description = lc.legalinfo.description,
        subject = lc.subject,
        contract = CompanyContract(
          number = lc.contractnumber,
          signingDate = lc.contractdate
        ),
        contact = CompanyContact(
          phones = Seq(Phone(countryCode = lc.contact.countrycode, number = lc.contact.phone)),
          address = lc.contact.address,
          legalAddress = lc.contact.legaladdress,
          url = lc.legalinfo.url
        ),
        management = CompanyManagement(
          officeName = lc.legalinfo.office,
          managerName = lc.legalinfo.manager,
          accountantName = lc.legalinfo.accountant
        ),
        legalInformation = CompanyInformation(
          companyStructure = lc.legalinfo.structure,
          ogrn = lc.legalinfo.ogrn,
          registrationDate = lc.legalinfo.registrationdate,
          registrator = lc.legalinfo.registrator,
          taxNumber = lc.legalinfo.taxnumber,
          kpp = lc.legalinfo.kpp,
          licenceInformation = lc.licence,
          companyType = CompanyType.Company
        ),
        companyMedia = lc.media.map(url => CompanyMedia(MediaObject(MediaObjectId(url), UrlType.MigrationType), Some("main"))).toSeq, //type is http
        partnershipType = PartnershipType.Partner,
        payment = CompanyPayment("-", "-", "-", None, None))
      val cmpMod = domain.PreModerationCompany(
        id = PreModerationCompanyId(),
        agentId = cmp.agentId,
        mainUserId = cmp.mainUserId,
        registrationDate = cmp.registrationDate,
        name = cmp.name,
        shortName = cmp.shortName,
        description = cmp.description,
        subject = cmp.subject,
        contract = cmp.contract,
        contact = cmp.contact,
        management = cmp.management,
        legalInformation = cmp.legalInformation,
        companyMedia = cmp.companyMedia,
        moderationStatus = PreModerationStatus(
          adminUser = None,
          status = ModerationStatusId.Active,
          message = Some("Migration"),
          time = Some(new Date)
        ),
        payment = cmp.payment,
        partnershipType = cmp.partnershipType,
        publishCompany = Some(cmp.id)
      )
      lc.media.foreach {
        url =>
          saveUrl(EntityType.company, cmp.id.id, url, UrlField.CompanyMain)
          saveUrl(EntityType.companyModeration, cmpMod.id.id, url, UrlField.CompanyMain)
      }
      (cmp, cmpMod)
    }
  }

  def convertLegacyMainUser(mainUser: LegacyCompanyMainUser, companyId: CompanyId) = {
    val user = AdminUser(name = mainUser.name, lastName = mainUser.lastname, role = AdminRoleId.Client, companyId = Some(companyId))
    val creds = AdminCredentials(adminUserId = user.id, userName = mainUser.email, password = "", legacyPassword = Option(mainUser.pwd))
    (user, creds)
  }

  def saveCompany(company: Company, lid: Int): Future[Company] = {
    for {
      cmp <- companyDataActor.ask(CompanyDataActor.SaveCompany(company)).mapTo[Company]
      _ <- idMappingDataActor.ask(IdMappingDataActor.SaveIdMapping(IdMapping(new ObjectId, MappingCollection.company, lid, cmp.id.id)))
    } yield cmp
  }

  def saveModeraion(mod: PreModerationCompany): Future[Any] = {
    preModerationCompanyDataActor ? PreModerationCompanyDataActor.SaveCompany(mod)
  }

  def saveAdminUser(user: AdminUser) =
    adminUserDataActor.ask(AdminUserDataActor.SaveAdminUser(user)).mapTo[AdminUser]

  def saveAdminCredentials(creds: AdminCredentials) =
    adminCredentialsDataActor.ask(AdminCredentialsDataActor.SaveAdminCredentials(creds)).mapTo[AdminCredentials]

  lazy val preModerationCompanyDataActor = lookup(PreModerationCompanyDataActor.Id)
  lazy val companyDataActor = lookup(CompanyDataActor.Id)
  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)
  lazy val adminCredentialsDataActor = lookup(AdminCredentialsDataActor.Id)
}
