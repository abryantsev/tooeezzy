package com.tooe.core.db.mongo.domain

import com.tooe.core.domain._
import java.util.Date
import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.CurrencyId
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.AdminUserId
import com.tooe.core.util.MediaHelper._

trait CompanyIdProjection {
  def id: CompanyId
}

@Document( collection = "company" )
case class Company(id: CompanyId = CompanyId(),
                    agentId: AdminUserId,
                    mainUserId: AdminUserId,
                    registrationDate: Date = new Date,
                    name: String,
                    shortName: Option[String] = None,
                    description: String,
                    subject: Option[String],
                    contract: CompanyContract,
                    contact: CompanyContact,
                    management: CompanyManagement,
                    legalInformation: CompanyInformation,
                    companyMedia: Seq[CompanyMedia] = Nil,
                    payment: CompanyPayment,
                    partnershipType: PartnershipType,
                    synchronizationTime: Option[Date] = None,
                    updateTime: Option[Date] = None)
  extends CompanyIdProjection {

  def getMainCompanyMediaOpt = companyMedia.find(_.purpose == Some("main"))

  def getMainCompanyMediaUrl(imageSize: String) = getMainCompanyMediaOpt.map(_.url).asMediaUrl(imageSize, CompanyDefaultUrlType)

}

case class CompanyContract(number: String, signingDate: Date)

case class CompanyContact(phones: Seq[Phone], address: String, legalAddress: String, url: Option[String] = None)

case class CompanyManagement(officeName: String, managerName: String, accountantName: String)

case class CompanyInformation(companyStructure: String,
                              ogrn: Option[String],
                              registrationDate: Option[Date],
                              registrator: Option[String],
                              taxNumber: String,
                              kpp: Option[String],
                              licenceInformation: Option[String],
                              companyType: CompanyType)

case class CompanyMedia(url: MediaObject, purpose: Option[String])

case class  CompanyPayment(bankAccount: String,
                          bik: String,
                          transferAccount: String,
                          frequency: Option[PaymentPeriod],
                          limit: Option[PaymentLimit])

case class PaymentLimit(value: BigDecimal, currency: CurrencyId)