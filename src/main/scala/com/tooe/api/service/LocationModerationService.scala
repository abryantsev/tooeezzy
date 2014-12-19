package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.domain._
import com.tooe.core.usecase.location.{ModerationLocationSearchSortType, PreModerationLocationWriteActor}
import com.tooe.core.usecase.{PhoneValidator, PreModerationLocationReadActor, ModerationLocationsSearchRequest, UpdateLocationRequest}
import spray.http.StatusCodes
import spray.routing.PathMatcher
import com.tooe.core.exceptions.ApplicationException
import com.tooe.api.validation.{ValidationContext, Validatable}

class LocationModerationService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider{

  import LocationModerationService._

  lazy val preModerationLocationWriteActor = lookup(PreModerationLocationWriteActor.Id)
  lazy val preModerationLocationReadActor = lookup(PreModerationLocationReadActor.Id)

  val adminSearchOffsetLimit = getOffsetLimit(defaultLimit = 10)

  val route =
    (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
      authenticateAdminBySession { implicit adminUserSession: AdminUserSession =>
        pathEndOrSingleSlash {
          post {
            authorizeByRole(AdminRoleId.Client) {
              entity(as[SaveLocationRequest]) { lsr: SaveLocationRequest =>
                val companyId = adminUserSession.companies.headOption.getOrElse(throw ApplicationException(message = "Invalid admin user"))
                complete(StatusCodes.Created, (preModerationLocationWriteActor ? PreModerationLocationWriteActor.SaveLocation(companyId, lsr, routeContext.lang)).mapTo[SuccessfulResponse])
              }
            }
          }
        } ~
        (path("search") & parameters('name.as[String] ?, 'modstatus.as[ModerationStatusId] ?, 'sort.as[ModerationLocationSearchSortType] ?) & adminSearchOffsetLimit).as(ModerationLocationsSearchRequest){ request: ModerationLocationsSearchRequest =>
          get{
            authorizeByRole(AdminRoleId.Client) {
              complete(preModerationLocationReadActor.ask(PreModerationLocationReadActor.ModerationLocationsSearch(request, adminUserSession.companies, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        } ~
        pathPrefix(ObjectId).as(PreModerationLocationId) { preModerationLocationId: PreModerationLocationId =>
          pathEndOrSingleSlash {
            post {
              (authorizeByRole(AdminRoleId.Client) & authorizeByResource(preModerationLocationId)) {
                entity(as[UpdateLocationRequest]) { ulr: UpdateLocationRequest =>
                  complete {
                    preModerationLocationWriteActor.ask(PreModerationLocationWriteActor.UpdateLocation(preModerationLocationId, ulr, routeContext)).mapTo[SuccessfulResponse]
                  }
                }
              }
            }
          } ~
            path("moderation") {
              post {
                authorizeByRole(AdminRoleId.Moderator) {
                  entity(as[LocationModerationRequest]) { request: LocationModerationRequest =>
                    complete(preModerationLocationWriteActor.ask(PreModerationLocationWriteActor.UpdateModerationStatus(preModerationLocationId, request, adminUserSession.adminUserId, routeContext)).mapTo[SuccessfulResponse])
                  }
                }
              }
            }
        }
        }
      }
}

object LocationModerationService {
  val Root = PathMatcher("locationsmoderation")
}

case class SaveLocationRequest( name: String,
                                @JsonProperty("openinghours") openingHours: String,
                                description: String,
                                address: SaveLocationAddressItem,
                                categories: Seq[LocationCategoryId],
                                @JsonProperty("coords") coordinates: Coordinates,
                                @JsonProperty("countrycode") countryCode: Option[String],
                                phone: Option[String],
                                media: Option[MediaUrl],
                                url: Option[String]) extends UnmarshallerEntity with Validatable with PhoneValidator {



  def validate(ctx: ValidationContext) {

    if(name.isEmpty || phone.isEmpty)
      ctx.fail("Set phone and country code")
    validatePhone(ctx, phone, countryCode)

  }

}

case class SaveLocationAddressItem(@JsonProperty("regionid") regionId: RegionId,
                                    street: String) extends UnmarshallerEntity

case class LocationModerationRequest(@JsonProperty("modstatus") status: ModerationStatusId,
                                    @JsonProperty("modmessage") message: Option[String]) extends UnmarshallerEntity with Validatable {
  override def validate(ctx: ValidationContext) = if (!ModerationStatusId.statuses.contains(status)) ctx.fail("Invalid status value")
}