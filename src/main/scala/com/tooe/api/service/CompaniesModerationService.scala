package com.tooe.api.service

import spray.routing.PathMatcher
import akka.actor.ActorSystem
import akka.pattern._
import com.tooe.core.domain._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.company.{ModerationCompanyReadActor, ModerationCompanyWriteActor}
import spray.http.StatusCodes
import java.util.Date
import com.tooe.core.usecase.company.ModerationCompanyWriteActor._
import com.tooe.api.validation.{ValidationContext, Validatable}

class CompaniesModerationService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import CompaniesModerationService._
  import AdminRoleId._

  lazy val moderationCompanyWriteActor = lookup(ModerationCompanyWriteActor.Id)
  lazy val moderationCompanyReadActor = lookup(ModerationCompanyReadActor.Id)

  val moderationSearchParameters = parameters('companyname ?, 'modstatus.as[ModerationStatusId] ?, 'sort ?).as(SearchModerationCompanyRequest)

  val searchOffsetLimit = parameters('offset.as[Int] ?, 'limit.as[Int] ?) as ((offset: Option[Int], limit: Option[Int]) => OffsetLimit(offset, limit, 0, 10))

  val route = (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
    authenticateAdminBySession { implicit adminSession: AdminUserSession =>
      pathEndOrSingleSlash {
        post {
          authorizeByRole(Agent | Dealer | Superagent | Superdealer) {
            entity(as[CompanyCreateRequest]) { request: CompanyCreateRequest =>
              complete(StatusCodes.Created,
                (moderationCompanyWriteActor ? CreateCompany(request)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      path("admsearch") {
        authenticateAdminBySession { implicit adminSession: AdminUserSession =>
          (get & searchOffsetLimit & moderationSearchParameters) { (offsetLimit: OffsetLimit, request: SearchModerationCompanyRequest) =>
            authorizeByRole(Moderator) {
              complete((moderationCompanyReadActor ? ModerationCompanyReadActor.SearchModerationCompany(request, offsetLimit)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      pathPrefix(Segment).as(PreModerationCompanyId) { preModerationCompanyId: PreModerationCompanyId =>
        path("moderation") {
          post {
            entity(as[CompanyModerationRequest]) { request: CompanyModerationRequest =>
              authorizeByRole(AdminRoleId.Moderator) {
                complete((moderationCompanyWriteActor ? UpdateModerationStatus(preModerationCompanyId, request, adminSession.adminUserId, routeContext)).mapTo[SuccessfulResponse])
              }
            }
          }
        } ~
        pathEndOrSingleSlash {
          get {
            authorizeByRole(AdminRoleId.Client | AdminRoleId.Moderator) {
              complete((moderationCompanyReadActor ? ModerationCompanyReadActor.FindPreModerationCompany(preModerationCompanyId)).mapTo[SuccessfulResponse])
            }
          } ~
          post {
            entity(as[CompanyChangeRequest]) { request: CompanyChangeRequest =>
              authorizeByRole(AdminRoleId.Client) {
                complete((moderationCompanyWriteActor ? ChangeCompany(preModerationCompanyId, request, routeContext.lang)).mapTo[SuccessfulResponse])
              }
            }
          }
        } ~
        path("media") {
          post {
            authorizeByRole(Agent | Dealer | Superagent | Superdealer) {
              entity(as[UpdateCompanyMediaRequest]) { request: UpdateCompanyMediaRequest =>
                complete(moderationCompanyWriteActor.ask(ModerationCompanyWriteActor.UpdateCompanyMedia(preModerationCompanyId, request.media)).mapTo[SuccessfulResponse])
              }
            }
          }
        }
      } ~
      pathPrefix("company") {
        path(Segment).as(CompanyId) { companyId: CompanyId =>
          ((authorizeByRole(AdminRoleId.Client) & authorizeByResource(companyId)) | authorizeByRole(AdminRoleId.Moderator)) {
            complete((moderationCompanyReadActor ? ModerationCompanyReadActor.FindPreModerationCompanyByCompanyId(companyId)).mapTo[SuccessfulResponse])
          }
        }
      }
    }
  }
}

object CompaniesModerationService {
  val Root = PathMatcher("companiesmoderation")
}

case class UpdateCompanyMediaRequest(media: MediaObjectId) extends UnmarshallerEntity

case class CompanyModerationRequest(@JsonProperty("modstatus") status: ModerationStatusId,
                                    @JsonProperty("modmessage") message: Option[String]) extends UnmarshallerEntity

case class CompanyCreateRequest(@JsonProperty("agentid") agentId: AdminUserId,
                                @JsonProperty("contractnumber") contractNumber: String,
                                @JsonProperty("contractdate") contractDate: Date,
                                @JsonProperty("companyname") companyName: String,
                                @JsonProperty("countrycode") countryCode: String,
                                phone: String,
                                management: CompanyManagementItem,
                                description: String,
                                url: Option[String],
                                address: String,
                                @JsonProperty("legalinfo") legalInfo: CompanyLegalInfo,
                                media: Option[String],
                                @JsonProperty("mainuser") user: MainUser,
                                @JsonProperty("partnershiptype") partnershipType: PartnershipType,
                                @JsonProperty("legaltype") legalType: CompanyType,
                                @JsonProperty("companyshortname") shortName: Option[String],
                                payment: CompanyPaymentItem) extends UnmarshallerEntity with Validatable {

  def validate(ctx: ValidationContext) =
    if (partnershipType == PartnershipType.Dealer && !user.role.exists(role => role == AdminRoleId.Dealer || role == AdminRoleId.Superdealer)) {
      ctx.fail("role should be (super)dealer if partnership is dealer")
    }

}


case class CompanyChangeRequest(@JsonProperty("agentid") agentId: Option[AdminUserId],
                                @JsonProperty("contractnumber") contractNumber: Option[String],
                                @JsonProperty("contractdate") contractDate: Option[Date],
                                @JsonProperty("companyname") companyName: Option[String],
                                @JsonProperty("countrycode") countryCode: Option[String],
                                phone: Option[String],
                                management: Option[CompanyChangeManagement],
                                description: Option[String],
                                url: Unsetable[String],
                                address: Option[String],
                                @JsonProperty("legalinfo") legalInfo: Option[CompanyChangeLegalInfo],
                                media: Option[String],
                                @JsonProperty("companyshortname") shortName: Unsetable[String],
                                payment: Option[CompanyChangePayment]) extends UnmarshallerEntity

case class SearchModerationCompanyRequest(name: Option[String],
                                          status: Option[ModerationStatusId],
                                          sort: Option[PreModerationSort])