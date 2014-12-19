package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern._
import spray.routing.PathMatcher
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.usecase._
import com.tooe.core.domain.CompanyId
import com.tooe.core.db.mongo.domain.CompanyManagement
import com.tooe.core.domain.CurrencyId
import com.tooe.core.usecase.CompanyReadActor.SearchModerationCompany
import com.tooe.api.validation.{ValidationHelper, ValidationContext, Validatable}
import AdminRoleId._

class CompaniesService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import CompaniesService._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val companyWriteActor = lookup(CompanyWriteActor.Id)
  lazy val companyReadActor = lookup(CompanyReadActor.Id)

  val searchOffsetLimit = parameters('offset.as[Int] ?, 'limit.as[Int] ?) as ((offset: Option[Int], limit: Option[Int]) => OffsetLimit(offset, limit, 0, 10))

  val searchParameters = parameters('companyname ?, 'sort ?).as(SearchCompanyRequest)

  val route =
    (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
      path("admsearch") {
        authenticateAdminBySession { implicit adminSession: AdminUserSession =>
        (get & searchOffsetLimit & searchParameters) { (offsetLimit: OffsetLimit, request: SearchCompanyRequest) =>
          authorizeByRole(Moderator | Superagent | Agent | Superdealer | Dealer) {
            ValidationHelper.checkObject(request)
            complete((companyReadActor ? SearchModerationCompany(request, offsetLimit)).mapTo[SuccessfulResponse])
          }
        }
        }
      }
    } ~
    (mainPrefix & path(Root/Segment).as(CompanyId)) { (routeContext: RouteContext, companyId: CompanyId) =>
      get {
        authenticateAdminBySession { implicit s: AdminUserSession =>
          authorizeByRole(Moderator | Superagent | Agent | Superdealer | Dealer | Client) {
            complete((companyReadActor ? CompanyReadActor.GetCompany(companyId)).mapTo[SuccessfulResponse])
          }
        }
      }
    }
}

object CompaniesService {
  val Root = PathMatcher("companies")
}

case class MainUser(email: String,
                    @JsonProperty("pwd") password: String,
                    name: String,
                    @JsonProperty("lastname") lastName: String,
                    role: Option[AdminRoleId])

case class PaymentsDetails(value: BigDecimal, currency: CurrencyId)

case class CompanyLegalInfo(structure: String,
                            ogrn: Option[String],
                            @JsonProperty("registrationdate") registrationDate: Option[Date],
                            registrator: Option[String],
                            @JsonProperty("taxnumber") taxNumber: String,
                            kpp: Option[String],
                            @JsonProperty("legaladdress") legalAddress: String,
                            subject: Option[String],
                            licence: Option[String])

case class CompanyManagementItem(office: String,
                                 manager: String,
                                 accountant: String)

object CompanyManagementItem {

  def apply(management: CompanyManagement): CompanyManagementItem =
    CompanyManagementItem(
      office = management.officeName,
      manager = management.managerName,
      accountant = management.accountantName
    )
}

case class CompanyPaymentItem(@JsonProperty("baccount") bankAccount: String,
                          @JsonProperty("bindex") bankIndex: String,
                          @JsonProperty("btransferaccount") bankTransferAccount: String,
                          period: Option[PaymentPeriod],
                          limit: Option[PaymentsDetails])

case class CompanyChangeManagement(office: Option[String],
                             manager: Option[String],
                             accountant: Option[String])

case class CompanyChangeLegalInfo(structure: Option[String],
                            ogrn: Unsetable[String],
                            @JsonProperty("registrationdate") registrationDate: Unsetable[Date],
                            registrator: Unsetable[String],
                            @JsonProperty("taxnumber") taxNumber: Option[String],
                            kpp: Unsetable[String],
                            @JsonProperty("legaladdress") legalAddress: Option[String],
                            subject: Unsetable[String],
                            licence: Unsetable[String])

case class CompanyChangePayment(@JsonProperty("baccount") bankAccount: Option[String],
                          @JsonProperty("bindex") bankIndex: Option[String],
                          @JsonProperty("btransferaccount") bankTransferAccount: Option[String],
                          period: Unsetable[PaymentPeriod],
                          limit: Unsetable[PaymentsDetails])

case class SearchCompanyRequest(name: Option[String],
                              sort: Option[CompanySort]) extends Validatable {

  def validate(ctx: ValidationContext) {
    name.map { name =>
      if(name.length < 3)
        ctx.fail("Companyname length must be greater that 3")
    }
  }

}