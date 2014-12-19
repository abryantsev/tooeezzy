package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import java.util.{UUID, Date}
import com.tooe.core.domain._
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.db.mongo.domain.PaymentLimit
import com.tooe.core.domain.AdminUserId
import com.tooe.core.domain.CompanyId
import com.tooe.core.db.mongo.domain.CompanyPayment
import com.tooe.core.db.mongo.domain.Phone
import com.tooe.core.db.mongo.domain.CompanyInformation
import com.tooe.core.db.mongo.domain.CompanyManagement
import com.tooe.core.db.mongo.domain.CompanyContract
import com.tooe.core.db.mongo.domain.CompanyMedia
import com.tooe.core.db.mongo.domain.Company
import com.tooe.core.domain.CurrencyId
import com.tooe.core.db.mongo.domain.CompanyContact
import com.tooe.api.service.{OffsetLimit, SearchCompanyRequest}

class CompanyDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: CompanyDataService = _
  lazy val entities = new MongoDaoHelper("company")

  @Test
  def saveAndRead {
    val entity = new CompanyFixture().company
    service.find(entity.id) === None
    service.save(entity)
    service.find(entity.id) === Some(entity)
  }

  @Test
  def saveAndReadAll {
    val entities = (1 to 5).map(_ => new CompanyFixture().company).toSeq
    entities.foreach(service.save)
    val result = service.findAllByIds(entities.map(_.id))
    result.size === entities.size
    result.zip(entities).foreach{
      case (f,e) => f === e
    }
  }

  @Test
  def representation {
    val entity = new CompanyFixture().company
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
       "p" : {
          "ba" : "${entity.payment.bankAccount}",
          "bi" : "${entity.payment.bik}",
          "bt" : "${entity.payment.transferAccount}",
          "p" : "${entity.payment.frequency.map(_.id).getOrElse("")}",
          "l" : { "v" : ${entity.payment.limit.map(_.value).getOrElse(BigDecimal(0))}, "c" : "${entity.payment.limit.map(_.currency.id).getOrElse("")}" }
        },
        "pt" : "${entity.partnershipType.id}",
        "st" : ${entity.synchronizationTime.get.mongoRepr},
        "ut" : ${entity.updateTime.get.mongoRepr}
        }
    }""")
  }

  @Test
  def exportedCompaniesComplete {

    val entity = new CompanyFixture().company.copy().copy(synchronizationTime = None)
    service.save(entity)
    service.exportedCompaniesComplete(Seq(entity.id))
    service.find(entity.id).flatMap(_.synchronizationTime) !== None
  }

  @Test
  def getExportedCompanies {

    val companies = service.getExportedCompanies()
    companies.foreach(c => c.synchronizationTime === None)
    companies.size must beLessThanOrEqualTo(100)

  }

  @Test
  def search {
    val entity = new CompanyFixture().company
    service.save(entity)
    val request = SearchCompanyRequest(name = Some(entity.name), None)
    service.search(request, OffsetLimit()) === Seq(entity)
    service.searchCount(request) === 1
  }

  @Test
  def updateMediaStorageToS3 {
    val media1 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val media2 = new MediaObjectFixture(storage = UrlType.http).mediaObject

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    val entity = new CompanyFixture().company.copy(companyMedia = Seq(media1, media2).map(CompanyMedia(_, None)))
    service.save(entity)

    service.updateMediaStorageToS3(entity.id, media2.url, expectedMedia.url)

    service.find(entity.id).map(_.companyMedia.map(_.url)) === Some(Seq(media1, expectedMedia))

  }

  @Test
  def updateMediaStorageToCDN {
    val media1 = new MediaObjectFixture().mediaObject
    val media2 = new MediaObjectFixture().mediaObject

    val entity = new CompanyFixture().company.copy(companyMedia = Seq(media1, media2).map(CompanyMedia(_, None)))
    service.save(entity)

    service.updateMediaStorageToCDN(entity.id, media2.url)

    service.find(entity.id).map(_.companyMedia.map(_.url)) === Some(Seq(media1, media2.copy(mediaType = None)))

  }

  @Test
  def findCompaniesByAgentUserId {
    val agentId = AdminUserId()
    val c1, c2 = new CompanyFixture(agentId = agentId).company
    val c3 = new CompanyFixture().company

    Seq(c1, c2, c3) foreach service.save

    val foundCompanies = service.findCompaniesByAgentUserId(agentId)
    foundCompanies.map(_.id) === Set(c1, c2).map(_.id)

    val underlyingEntity = foundCompanies.head.asInstanceOf[Company]
    underlyingEntity.agentId === null
    underlyingEntity.companyMedia === Nil
  }

  @Test
  def getCompanyMedia {
    val media = new MediaObjectFixture().mediaObject
    val entity = new CompanyFixture().company.copy(companyMedia = Seq(media).map(CompanyMedia(_, None)))
    service.save(entity)

    service.find(entity.id).map(_.companyMedia.map(_.url.url)) === Some(Seq(media.url))
  }
}

class CompanyFixture(agentId: AdminUserId = AdminUserId()) {
  val company = Company(
      id = CompanyId(),
      name = "name:" + UUID.randomUUID().toString,
      agentId = agentId,
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
      synchronizationTime = Some(new Date(2013, 10, 11)),
      updateTime = Some(new Date(2013, 11, 10))
    )
}