package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.core.usecase.company._
import com.tooe.api.service._
import java.util.Date
import com.tooe.api._
import org.bson.types.ObjectId
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.admin_user.AdminUserDataActor
import com.tooe.core.db.mongo.domain.PaymentLimit
import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.usecase.admin_user.AdminUserDataActor.GetAdminUsers
import com.tooe.core.db.mongo.domain.CompanyPayment
import com.tooe.core.db.mongo.domain.Company
import com.tooe.core.db.mongo.domain.PreModerationCompany
import scala.concurrent.Future
import com.tooe.core.domain.MediaUrl
import com.tooe.core.util.Images
import com.tooe.core.util.MediaHelper._

object CompanyReadActor {

  final val Id = Actors.CompanyRead

  case class GetCompany(companyId: CompanyId)
  case class GetExportedCompanies()
  case class SearchModerationCompany(request: SearchCompanyRequest, offsetLimit: OffsetLimit)
  case class GetAdminUserCompanyIds(adminUserId: AdminUserId)
}

class CompanyReadActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import CompanyReadActor._

  lazy val companyDataActor = lookup(CompanyDataActor.Id)
  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)

  def receive = {
    case GetCompany(id) => (companyDataActor ? CompanyDataActor.GetCompany(id)).mapTo[Company] map (c => GetCompanyResponse(CompanyItem(c))) pipeTo sender
    case GetExportedCompanies =>
      (for {
        (companies, count) <- (companyDataActor ? CompanyDataActor.GetExportedCompanies)
          .zip(companyDataActor ? CompanyDataActor.ExportedCompaniesCount).mapTo[(Seq[Company], Long)]
      } yield {
        ExportedCompaniesResponse(count, companies map ExportedCompanyItem.apply)
      }) pipeTo sender

    case SearchModerationCompany(request, offsetLimit) =>
      (for {
        (companies, count) <- (companyDataActor ? CompanyDataActor.SearchCompany(request, offsetLimit))
                          .zip(companyDataActor ? CompanyDataActor.SearchCompanyCount(request)).mapTo[(Seq[Company], Long)]
        users <- (adminUserDataActor ? GetAdminUsers(companies.map(_.mainUserId) ++ companies.map(_.agentId))).mapTo[Seq[AdminUser]]
        usersMap = users.map(u => (u.id, Some(u))).toMap withDefaultValue None
      } yield {
        SearchCompanyResponse(count, companies map (c => SearchCompanyItem(c, usersMap)))
      }) pipeTo sender

    case GetAdminUserCompanyIds(agentId) =>
      val future = for {
        adminUser <- findAdminUser(agentId)
        companiesByAgent <- findCompanies(agentId)
      } yield companiesByAgent | adminUser.companyId.toSet
      future pipeTo sender
  }

  def findAdminUser(agentId: AdminUserId): Future[AdminUser] =
    (adminUserDataActor ? AdminUserDataActor.FindAdminUser(agentId)).mapTo[AdminUser]

  def findCompanies(agentId: AdminUserId): Future[Set[CompanyId]] =
    (companyDataActor ? CompanyDataActor.FindCompaniesByAgentUserId(agentId)).mapTo[Set[CompanyId]]
}

case class CompanyItem(id: ObjectId,
                       @JsonProp("agentid") agentId:ObjectId,
                       @JsonProp("contractnumber") contractNumber: String,
                       @JsonProp("contractdate") contractDate: Date,
                       @JsonProp("companyname") companyName: String,
                       @JsonProp("countrycode") countryCode: Option[String],
                       phone: Option[String],
                       management: CompanyManagementItem,
                       description: String,
                       url: Option[String],
                       address: String,
                       @JsonProp("legalinfo") legalInfo: LegalInfoItem,
                       media: Option[MediaUrl],
                       @JsonProp("partnershiptype") partnershipType: String,
                       @JsonProp("legaltype") legalType: String,
                       @JsonProp("companyshortname") shortName: Option[String],
                       payment: CompanyPaymentItem)

object CompanyItem{
  def apply(company: Company): CompanyItem = {

    def mainOrFirstElement[T](seq: Seq[T])( mainCheckerField: T => Option[String]): Option[T] = {
      seq.find(mainCheckerField(_) == Some("main")).orElse(seq.headOption)
    }

    val phones = company.contact.phones
    val contactPhone = mainOrFirstElement(phones)(_.purpose)
    val medias = company.companyMedia
    val companyMedia = mainOrFirstElement(medias)(_.purpose)

    CompanyItem(id = company.id.id,
                agentId = company.agentId.id,
                contractNumber = company.contract.number,
                contractDate = company.contract.signingDate,
                companyName = company.name,
                countryCode = contactPhone.map(_.countryCode),
                phone = contactPhone.map(_.number),
                management = CompanyManagementItem(company.management),
                description = company.description,
                url = company.contact.url,
                address = company.contact.address,
                legalInfo = LegalInfoItem(company),
                media = companyMedia.map(_.url.asMediaUrl(Images.AdminCompany.Full.Self.Media)),
                partnershipType = company.partnershipType.id,
                legalType = company.legalInformation.companyType.id,
                shortName = company.shortName,
                payment = CompanyPaymentItem(company.payment)
               )
  }
}

case class LegalInfoItem (structure: String,
                          ogrn: Option[String],
                          @JsonProp("registrationdate") registrationDate: Option[Date],
                          registrator: Option[String],
                          @JsonProp("taxnumber") taxNumber: String,
                          kpp: Option[String],
                          @JsonProp("legaladdress") legalAddress: String,
                          subject: Option[String],
                          licence: Option[String])

object LegalInfoItem {

  def apply(company: Company): LegalInfoItem = {
    val legalInformation = company.legalInformation
    LegalInfoItem(structure = legalInformation.companyStructure,
                  ogrn = legalInformation.ogrn,
                  registrationDate = legalInformation.registrationDate,
                  registrator = legalInformation.registrator,
                  taxNumber = legalInformation.taxNumber,
                  kpp = legalInformation.kpp,
                  legalAddress = company.contact.legalAddress,
                  subject = company.subject,
                  licence = legalInformation.licenceInformation
                 )
  }

  def apply(company: PreModerationCompany): LegalInfoItem = {
    val legalInformation = company.legalInformation
    LegalInfoItem(structure = legalInformation.companyStructure,
      ogrn = legalInformation.ogrn,
      registrationDate = legalInformation.registrationDate,
      registrator = legalInformation.registrator,
      taxNumber = legalInformation.taxNumber,
      kpp = legalInformation.kpp,
      legalAddress = company.contact.legalAddress,
      subject = company.subject,
      licence = legalInformation.licenceInformation
    )
  }
}

case class CompanyPaymentItem (@JsonProp("baccount") bankAccount: String,
                               @JsonProp("bindex") bik: String,
                               @JsonProp("btransferaccount") transferAccount: String,
                               @JsonProp("period") paymentFrequency: Option[String],
                               limit: Option[PaymentLimit])

object CompanyPaymentItem {
  def apply(payment: CompanyPayment): CompanyPaymentItem =
    CompanyPaymentItem(bankAccount = payment.bankAccount,
                       bik = payment.bik,
                       transferAccount = payment.transferAccount,
                       paymentFrequency = payment.frequency.map(_.id),
                       limit = payment.limit)
}

case class ExportedCompaniesResponse(@JsonProperty("companiesexportcount") count: Long,
                                     @JsonProperty("companiesexport") companies: Seq[ExportedCompanyItem]) extends SuccessfulResponse

case class ExportedCompanyItem(id: CompanyId,
                               legaltype: CompanyType,
                               @JsonProperty("companyname") name: String,
                               @JsonProperty("companyshortname") shortName: Option[String],
                               manager: String,
                               @JsonProperty("legaladdress") legalAddress: String,
                               address: String,
                               phone: String,
                               @JsonProperty("taxnumber") taxNumber: String,
                               kpp: String,
                               @JsonProperty("registrationdate") registrationDate: Date,
                               licence: Option[String],
                               @JsonProperty("contractnumber") contractNumber: String,
                               @JsonProperty("contractdate") contractDate: Date,
                               payment: PaymentItem)

object ExportedCompanyItem {

  def apply(company: Company): ExportedCompanyItem =
    ExportedCompanyItem(
      id = company.id,
      legaltype = company.legalInformation.companyType,
      name = company.name,
      shortName = company.shortName,
      manager = company.management.managerName,
      legalAddress = company.contact.legalAddress,
      address = company.contact.address,
      phone = company.contact.phones.find(_.purpose == Some("main")).map(p => s"${p.countryCode} ${p.number}").getOrElse(""),
      taxNumber = company.legalInformation.taxNumber,
      kpp = company.legalInformation.kpp.getOrElse(""),
      registrationDate = company.registrationDate,
      licence = company.legalInformation.licenceInformation,
      contractNumber = company.contract.number,
      contractDate = company.contract.signingDate,
      payment = PaymentItem(company.payment)
    )

}

case class PaymentItem(@JsonProperty("baccount") bankAccount: String,
                       @JsonProperty("bindex") bankIndex: String,
                       @JsonProperty("btransferaccount") transferAccount: String,
                       period: Option[PaymentPeriod],
                       limit: Option[PaymentLimitItem])

object PaymentItem {

  def apply(payment: CompanyPayment): PaymentItem =
    PaymentItem(
      bankAccount = payment.bankAccount,
      bankIndex = payment.bik,
      transferAccount = payment.transferAccount,
      period = payment.frequency,
      limit = payment.limit.map(PaymentLimitItem(_))
    )

}

case class PaymentLimitItem(value: BigDecimal, currency: CurrencyId)

object PaymentLimitItem {

  def apply(paymentLimit: PaymentLimit): PaymentLimitItem = PaymentLimitItem(paymentLimit.value, paymentLimit.currency)

}

case class SearchCompanyResponse(@JsonProperty("companiescount") count: Long, companies: Seq[SearchCompanyItem]) extends SuccessfulResponse

case class SearchCompanyItem(id: CompanyId,
                                  agent: Option[ModerationCompanyAgent],
                                  @JsonProperty("companyname") name: String,
                                  @JsonProperty("contractnumber") contractNumber: String,
                                  @JsonProperty("contractdate") contractDate: Date,
                                  @JsonProperty("mainuser") mainUser: Option[ModerationCompanyMainUser],
                                  media: MediaUrl)

object SearchCompanyItem {

  def apply(company: Company, userMap: Map[AdminUserId, Option[AdminUser]]): SearchCompanyItem =
    SearchCompanyItem(id = company.id,
                          agent = userMap(company.agentId).map(ModerationCompanyAgent(_)),
                          name = company.name,
                          contractNumber = company.contract.number,
                          contractDate = company.contract.signingDate,
                          media = company.getMainCompanyMediaUrl(Images.CompanyAdmSearch.Full.Self.Media),
                          mainUser = userMap(company.mainUserId).map(ModerationCompanyMainUser(_)))

}

case class ModerationCompanyAgent(@JsonProperty("agentid") agentId: AdminUserId, role: AdminRoleId)

object ModerationCompanyAgent {
  def apply(user: AdminUser): ModerationCompanyAgent = ModerationCompanyAgent(user.id, user.role)
}
case class ModerationCompanyMainUser(name: String, @JsonProperty("lastname") lastName: String)

object ModerationCompanyMainUser {
  def apply(user: AdminUser): ModerationCompanyMainUser = ModerationCompanyMainUser(user.name, user.lastName)
}

case class GetCompanyResponse(company: CompanyItem) extends SuccessfulResponse