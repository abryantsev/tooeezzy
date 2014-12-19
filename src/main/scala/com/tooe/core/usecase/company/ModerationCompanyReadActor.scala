package com.tooe.core.usecase.company

import com.tooe.core.usecase._
import com.tooe.core.application.Actors
import com.tooe.api.service.{OffsetLimit, CompanyManagementItem, SuccessfulResponse}
import org.bson.types.ObjectId
import com.tooe.api._
import java.util.Date
import com.tooe.core.usecase.admin_user.AdminUserDataActor
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.usecase.company.PreModerationCompanyDataActor.SearchCompany
import scala.Some
import com.tooe.core.usecase.company.PreModerationCompanyDataActor.SearchCompanyCount
import com.tooe.core.domain.MediaUrl
import com.tooe.core.usecase.admin_user.AdminUserDataActor.GetAdminUsers
import com.tooe.core.db.mongo.domain.PreModerationCompany
import com.tooe.api.service.SearchModerationCompanyRequest
import com.tooe.core.util.Images
import com.tooe.core.util.MediaHelper._

object ModerationCompanyReadActor {
  final val Id = Actors.ModerationCompanyReadActor

  case class FindPreModerationCompany(id: PreModerationCompanyId)
  case class FindPreModerationCompanyByCompanyId(id: CompanyId)
  case class SearchModerationCompany(request: SearchModerationCompanyRequest, offsetLimit: OffsetLimit)
}

class ModerationCompanyReadActor extends AppActor {
  import ModerationCompanyReadActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val preModerationCompanyDataActor = lookup(PreModerationCompanyDataActor.Id)
  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)

  def receive = {

    case FindPreModerationCompany(id) =>
      (preModerationCompanyDataActor ? PreModerationCompanyDataActor.FindCompany(id)).mapTo[PreModerationCompany].map { company =>
        PreModerationCompanyResponse(PreModerationCompanyItem(company))
      } pipeTo sender

    case FindPreModerationCompanyByCompanyId(id) =>
      (preModerationCompanyDataActor ? PreModerationCompanyDataActor.FindPreModerationCompanyByCompanyId(id)).mapTo[PreModerationCompany].map { company =>
        PreModerationCompanyResponse(PreModerationCompanyItem(company))
      } pipeTo sender

    case SearchModerationCompany(request, offsetLimit) =>
      (for {
        (companies, count) <- (preModerationCompanyDataActor ? SearchCompany(request, offsetLimit))
          .zip(preModerationCompanyDataActor ? SearchCompanyCount(request)).mapTo[(Seq[PreModerationCompany], Long)]
        users <- (adminUserDataActor ? GetAdminUsers(companies.map(_.mainUserId) ++ companies.map(_.agentId))).mapTo[Seq[AdminUser]]
        usersMap = users.map(u => (u.id, Some(u))).toMap withDefaultValue None
      } yield {
        SearchModerationCompanyResponse(count, companies map (c => ModerationCompanyItem(c, usersMap)))
      }) pipeTo sender
  }

}

case class PreModerationCompanyResponse(company: PreModerationCompanyItem) extends SuccessfulResponse

case class PreModerationCompanyItem(id: PreModerationCompanyId,
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
                                    payment: CompanyPaymentItem,
                                    moderation: ModerationStatusItem)

object PreModerationCompanyItem{
  def apply(company: PreModerationCompany): PreModerationCompanyItem = {

    def mainOrFirstElement[T](seq: Seq[T])( mainCheckerField: T => Option[String]): Option[T] = {
      seq.find(mainCheckerField(_) == Some("main")).orElse(seq.headOption)
    }

    val phones = company.contact.phones
    val contactPhone = mainOrFirstElement(phones)(_.purpose)
    val medias = company.companyMedia
    val companyMedia = mainOrFirstElement(medias)(_.purpose)

    PreModerationCompanyItem(id = company.id,
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
      media = companyMedia.map(cm => cm.url.asMediaUrl(Images.CompanyModeration.Full.Self.Media)),
      partnershipType = company.partnershipType.id,
      legalType = company.legalInformation.companyType.id,
      shortName = company.shortName,
      payment = CompanyPaymentItem(company.payment),
      moderation = ModerationStatusItem(company.moderationStatus.status, company.moderationStatus.message)
    )
  }
}

case class SearchModerationCompanyResponse(@JsonProperty("companiescount") count: Long, companies: Seq[ModerationCompanyItem]) extends SuccessfulResponse

case class ModerationCompanyItem(id: PreModerationCompanyId,
                                 agent: Option[ModerationCompanyAgent],
                                 @JsonProperty("companyname") name: String,
                                 @JsonProperty("contractnumber") contractNumber: String,
                                 @JsonProperty("contractdate") contractDate: Date,
                                 @JsonProperty("mainuser") mainUser: Option[ModerationCompanyMainUser],
                                 media: MediaUrl,
                                 moderation: ModerationStatusItem,
                                 @JsonProperty("companyid") companyId: Option[CompanyId])

object ModerationCompanyItem {

  def apply(company: PreModerationCompany, userMap: Map[AdminUserId, Option[AdminUser]]): ModerationCompanyItem =
    ModerationCompanyItem(id = company.id,
      agent = userMap(company.agentId).map(ModerationCompanyAgent(_)),
      name = company.name,
      contractNumber = company.contract.number,
      contractDate = company.contract.signingDate,
      media = company.getMainCompanyMediaUrl(Images.CompanyModerationAdmSearch.Full.Self.Media),
      moderation = ModerationStatusItem(company.moderationStatus.status, company.moderationStatus.message),
      mainUser = userMap(company.mainUserId).map(ModerationCompanyMainUser(_)),
      companyId = company.publishCompany)

}