package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.domain._
import java.util.{UUID, Date}
import com.tooe.api.service._
import com.tooe.core.db.mongo.domain._
import scala.collection.mutable.ArrayBuffer
import com.tooe.core.db.mongo.domain.PaymentLimit
import com.tooe.core.db.mongo.domain.Phone
import com.tooe.core.domain.Unsetable.Update
import com.tooe.api.service.CompanyModerationRequest
import com.tooe.core.db.mongo.domain.CompanyInformation
import com.tooe.core.db.mongo.domain.CompanyManagement
import scala.Some
import com.tooe.core.db.mongo.domain.CompanyPayment
import com.tooe.api.service.PaymentsDetails
import com.tooe.core.db.mongo.domain.CompanyContract
import com.tooe.core.db.mongo.domain.CompanyMedia
import com.tooe.api.service.CompanyChangeLegalInfo
import com.tooe.api.service.CompanyChangeRequest
import com.tooe.api.service.CompanyChangePayment
import com.tooe.core.db.mongo.domain.CompanyContact
import com.tooe.core.db.mongo.domain.PreModerationCompany
import com.tooe.api.service.CompanyChangeManagement
import com.tooe.api.service.SearchModerationCompanyRequest
import com.tooe.core.db.mongo.domain.PaymentLimit
import com.tooe.core.domain.AdminUserId
import com.tooe.core.domain.CompanyId
import com.tooe.core.db.mongo.domain.Phone
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.db.mongo.domain.PreModerationStatus
import com.tooe.api.service.CompanyModerationRequest
import com.tooe.core.db.mongo.domain.CompanyInformation
import com.tooe.core.domain.PreModerationCompanyId
import com.tooe.core.db.mongo.domain.CompanyManagement
import scala.Some
import com.tooe.core.db.mongo.domain.CompanyPayment
import com.tooe.api.service.PaymentsDetails
import com.tooe.core.db.mongo.domain.CompanyContract
import com.tooe.core.db.mongo.domain.CompanyMedia
import com.tooe.core.domain.CurrencyId
import com.tooe.api.service.CompanyChangeLegalInfo
import com.tooe.api.service.CompanyChangeRequest
import com.tooe.api.service.CompanyChangePayment
import com.tooe.core.db.mongo.domain.CompanyContact
import com.tooe.core.db.mongo.domain.PreModerationCompany
import com.tooe.api.service.CompanyChangeManagement
import com.tooe.api.service.SearchModerationCompanyRequest

class PreModerationCompanyDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: PreModerationCompanyDataService = _
  lazy val entities = new MongoDaoHelper("company_mod")

  @Test
  def saveAndRead {
    val entity = new PreModerationCompanyFixture().company
    service.find(entity.id) === None
    service.save(entity)
    service.find(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new PreModerationCompanyFixture().company
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
       "_id" : ${entity.id.id.mongoRepr},
       "aid" : ${entity.agentId.id.mongoRepr},
       "mid" : ${entity.mainUserId.id.mongoRepr},
       "rt" : ${entity.registrationDate.mongoRepr},
       "n" : "${entity.name}" ,
       "d" : "${entity.description}",
       "su" : "${entity.subject.getOrElse("")}",
       "co" : {
          "nu": "${entity.contract.number}",
          "t" : ${entity.contract.signingDate.mongoRepr}
       },
       "c" : {
         "p" : [
            {
              "c" : "${entity.contact.phones.head.countryCode}",
              "n" : "${entity.contact.phones.head.number}",
              "p" : "${entity.contact.phones.head.purpose.getOrElse("")}"
            }
          ],
         "a" : "${entity.contact.address}",
         "la" : "${entity.contact.legalAddress}",
         "url" : "${entity.contact.url.getOrElse("")}"
       },
       "m" : {
          "on" : "${entity.management.officeName}",
          "mn" : "${entity.management.managerName}",
          "an" : "${entity.management.accountantName}"
       },
       "l" : {
          "cs" : "${entity.legalInformation.companyStructure}",
          "ogrn" : "${entity.legalInformation.ogrn.getOrElse("")}",
          "lrt" : ${entity.legalInformation.registrationDate.map(_.mongoRepr).getOrElse("")},
          "ri" : "${entity.legalInformation.registrator.getOrElse("")}",
          "tn" : "${entity.legalInformation.taxNumber}",
          "kpp" : "${entity.legalInformation.kpp.getOrElse("")}",
          "li" : "${entity.legalInformation.licenceInformation.getOrElse("")}",
          "lt" : "${entity.legalInformation.companyType.id}"
       },
       "cm" : [
          {
              "u" : { "mu" : "${entity.companyMedia.head.url.url.id}", "t" : "s3" } ,
              "p" : "${entity.companyMedia.head.purpose.getOrElse("")}"
          }
        ],
       "mod": { "s" : "${entity.moderationStatus.status.id}"} ,
       "p" : {
          "ba" : "${entity.payment.bankAccount}",
          "bi" : "${entity.payment.bik}",
          "bt" : "${entity.payment.transferAccount}",
          "p" : "${entity.payment.frequency.map(_.id).getOrElse("")}",
          "l" : { "v" : ${entity.payment.limit.map(_.value).getOrElse(BigDecimal(0))}, "c" : "${entity.payment.limit.map(_.currency.id).getOrElse("")}" }
        },
        "pt" : "${entity.partnershipType.id}",
        "puid" : ${entity.publishCompany.get.id.mongoRepr}
        }
    }""")
  }

  @Test
  def updateStatus {
    val entity = new PreModerationCompanyFixture().company
    service.save(entity)
    val request = CompanyModerationRequest(ModerationStatusId.Active, Some("moderation message"))
    val adminUserId = AdminUserId()
    val companyId = CompanyId()
    service.updateStatus(entity.id, request, adminUserId, Some(companyId))
    val updatedEntity = service.find(entity.id)
    updatedEntity.map(_.moderationStatus.status) === Some(request.status)
    updatedEntity.flatMap(_.moderationStatus.message) === request.message
    updatedEntity.flatMap(_.moderationStatus.adminUser) === Some(adminUserId)
    updatedEntity.flatMap(_.moderationStatus.time) !== None
    updatedEntity.flatMap(_.publishCompany) === Some(companyId)
  }

  @Test
  def getPublishCompanyId {
    val entity = new PreModerationCompanyFixture().company.copy(publishCompany = Some(CompanyId()))
    service.save(entity)
    service.getPublishCompanyId(entity.id) === entity.publishCompany
  }

  @Test
  def search {
    val entity = new PreModerationCompanyFixture().company
    service.save(entity)
    val request = SearchModerationCompanyRequest(name = Some(entity.name), None, None)
    service.search(request, OffsetLimit()) === Seq(entity)
    service.searchCount(request) === 1
  }

  @Test
  def changeCompany {

    val entity = new PreModerationCompanyFixture().company
    service.save(entity)
    val expectedEntity = entity.copy(
      agentId = AdminUserId(),
      contract = CompanyContract("new contract number", new Date),
      contact = CompanyContact(phones =  ArrayBuffer(Seq(Phone("new code", "new number", Some("main"))) :_*),
        address = "new address",
        legalAddress = "new legal adress",
        url = Some("new url")),
      legalInformation = CompanyInformation(companyStructure = "new structure",
        ogrn = Some("new ogrn"),
        registrationDate = Some(new Date()),
        registrator = Some("new registrator"),
        taxNumber = "new tax number",
        kpp = Some("new kpp"),
        licenceInformation = Some("new licence information"),
        companyType = CompanyType.Company),
      subject = Some("new subject"),
      companyMedia = ArrayBuffer(Seq(CompanyMedia(url = MediaObject("new_company_media_url"), purpose = Some("main"))) :_*),
      shortName = Some("new short name"),
      payment = CompanyPayment("new bank_account", "new bik", "new transfer_account", Some(PaymentPeriod.Day), Some(PaymentLimit(400.toDouble, CurrencyId("EUR")))),
      name = "new name",
      management = CompanyManagement(officeName = "new office name", managerName = "new manager name", accountantName = "new accountant name"),
      description = "new description",
      moderationStatus = PreModerationStatus().copy(status = ModerationStatusId.Waiting)
    )
    val request = CompanyChangeRequest(
      agentId =Some(expectedEntity.agentId),
      contractNumber = Some(expectedEntity.contract.number),
      contractDate = Some(expectedEntity.contract.signingDate),
      countryCode = expectedEntity.contact.phones.headOption.map(_.countryCode),
      phone = expectedEntity.contact.phones.headOption.map(_.number),
      url = Update(expectedEntity.contact.url.get),
      address = Some(expectedEntity.contact.address),
      media = expectedEntity.companyMedia.headOption.map(_.url.url.id),
      shortName = Update(expectedEntity.shortName.get),
      legalInfo = Some(CompanyChangeLegalInfo(structure = Some(expectedEntity.legalInformation.companyStructure),
        ogrn = Update(expectedEntity.legalInformation.ogrn.get),
        registrationDate = Update(expectedEntity.legalInformation.registrationDate.get),
        registrator = Update(expectedEntity.legalInformation.registrator.get),
        taxNumber = Some(expectedEntity.legalInformation.taxNumber),
        kpp = Update(expectedEntity.legalInformation.kpp.get),
        legalAddress = Some(expectedEntity.contact.legalAddress),
        subject = Update(expectedEntity.subject.get),
        licence = Update(expectedEntity.legalInformation.licenceInformation.get))),
      payment = Some(CompanyChangePayment(bankAccount = Some(expectedEntity.payment.bankAccount),
        bankIndex = Some(expectedEntity.payment.bik),
        bankTransferAccount = Some(expectedEntity.payment.transferAccount),
        period = Update(expectedEntity.payment.frequency.get),
        limit = Update(PaymentsDetails(expectedEntity.payment.limit.get.value, expectedEntity.payment.limit.get.currency))
      )),
      companyName = Some(expectedEntity.name),
      management = Some(CompanyChangeManagement(Some(expectedEntity.management.officeName),
        Some(expectedEntity.management.managerName),
        Some(expectedEntity.management.accountantName))),
      description = Some(expectedEntity.description)
    )
    service.changeCompany(entity.id, request)
    service.find(entity.id) === Some(expectedEntity)
  }

  @Test
  def findByCompanyId {
    val entity = new PreModerationCompanyFixture().company
    service.save(entity)
    service.findByCompanyId(entity.publishCompany.get) === Some(entity)
  }

  @Test
  def updateMediaStorageToS3 {
    val media1 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val media2 = new MediaObjectFixture(storage = UrlType.http).mediaObject

    val entity = new PreModerationCompanyFixture().company.copy(companyMedia = Seq(media1, media2).map(CompanyMedia(_, None)))
    service.save(entity)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(entity.id, media2.url, expectedMedia.url)

    service.find(entity.id).map(_.companyMedia.map(_.url)) === Some(Seq(media1, expectedMedia))

  }

  @Test
  def updateMediaStorageToCDN {
    val media1 = new MediaObjectFixture().mediaObject
    val media2 = new MediaObjectFixture().mediaObject

    val entity = new PreModerationCompanyFixture().company.copy(companyMedia = Seq(media1, media2).map(CompanyMedia(_, None)))
    service.save(entity)

    service.updateMediaStorageToCDN(entity.id, media2.url)

    service.find(entity.id).map(_.companyMedia.map(_.url)) === Some(Seq(media1, media2.copy(mediaType = None)))

  }

  @Test
  def updateMedia {
    val company = service.save(new PreModerationCompanyFixture().company)
    val media = CompanyMedia(new MediaObjectFixture().mediaObject, None)
    service.updateCompanyMedia(company.id, media)
    val result = service.find(company.id)
    result.map(_.companyMedia).getOrElse(Seq.empty) === Seq(media)
  }

}

class PreModerationCompanyFixture {
  val company = PreModerationCompany(
    id = PreModerationCompanyId(),
    name = "name:" + UUID.randomUUID().toString,
    agentId = AdminUserId(),
    mainUserId = AdminUserId(),
    registrationDate = new Date(),
    description = "description",
    subject = Some("subject"),
    contract = CompanyContract(number = "23123dgsdfg", signingDate = new Date()),
    contact = CompanyContact(phones =  Seq(Phone(countryCode = "+7", number = "912000001", purpose = Some("main"))),
      address = "some street",
      legalAddress = "legal street",
      url = Some("url")),
    management = CompanyManagement(officeName = "office name", managerName = "manager name", accountantName = "accountant name"),
    legalInformation = CompanyInformation(companyStructure = "LLC",
      ogrn = Some("ogrn"),
      registrationDate = Some(new Date()),
      registrator = Some("registrator"),
      taxNumber = "tax number",
      kpp = Some("kpp"),
      licenceInformation = Some("licence information"),
      companyType = CompanyType.Company),
    companyMedia = Seq(CompanyMedia(url = MediaObject("company_media_url"), purpose = Some("main"))),
    payment = CompanyPayment("bank_account", "bik", "transfer_account", Some(PaymentPeriod.Month), Some(PaymentLimit(4000, CurrencyId("RUR")))),
    partnershipType = PartnershipType.Partner,
    publishCompany = Some(CompanyId())
  )
}