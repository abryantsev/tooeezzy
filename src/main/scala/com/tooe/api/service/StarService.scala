package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern._
import com.tooe.core.usecase.{SecurityActor, UserReadActor, StarSubscriptionActor}
import spray.routing.PathMatcher
import com.tooe.core.domain._
import com.tooe.core.domain.UserId
import spray.http.StatusCodes

//import com.tooe.core.usecase.StarSubscriptionActor.GetUsersStars
import com.tooe.api.validation.{ValidationHelper, ValidationContext, Validatable}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.domain.{UserStar, Phone}

class StarService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import StarService._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val starSubscriptionActor = lookup(StarSubscriptionActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)
  lazy val securityActor = lookup(SecurityActor.Id)

  val starSearchParams = parameters('name ?, 'country.as[CountryId] ?, 'category.as[StarCategoryId] ?, 'sort ?, 'fields.as[CSV[StarField]] ?).as(SearchStarsRequest)

  val route =
    (mainPrefix & pathPrefix(Root)) { rc: RouteContext =>
      pathEndOrSingleSlash {
        post {
          authenticateAdminBySession { adminUserSession: AdminUserSession =>
            entity(as[StarRegistrationParams]) { request: StarRegistrationParams =>
              complete(StatusCodes.Created, (securityActor ? SecurityActor.RegisterStar(request, adminUserSession.adminUserId, rc)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      (path("search") & starSearchParams & offsetLimit) {
        (request: SearchStarsRequest, offsetLimit: OffsetLimit) =>
          get {
            authenticateBySession { userSession: UserSession =>
              ValidationHelper.checkObject(request)
              complete((userReadActor ? UserReadActor.SearchStars(request, offsetLimit, rc)).mapTo[SuccessfulResponse])
            }
          }
      } ~
      pathPrefix(Segment).as(UserId) { starId: UserId =>
        pathEnd {
        get {
          parameter('view.as[ShowType] ?) { viewType: Option[ShowType] =>
            authenticateBySession { userSession: UserSession =>
              complete((userReadActor ? UserReadActor.GetStar(userSession.userId, starId, viewType.getOrElse(ShowType.None))).mapTo[SuccessfulResponse])
            }
          }
        }
        } ~
        path(statistics) {
          get {
            parameters('statistics.as[CSV[StarStatisticField]] ?) { fields: Option[CSV[StarStatisticField]] =>
              complete((userReadActor ? UserReadActor.GetStarStatistics(starId, fields)).mapTo[SuccessfulResponse])
            }
          }
        }
      } ~
      path(topSearch) {
        get {
          complete((userReadActor ? UserReadActor.GetTopStars).mapTo[SuccessfulResponse])
        }
      }
    }

}

object StarService {
  val Root = PathMatcher("stars")
  val user = PathMatcher("user")
  val statistics = PathMatcher("statistics")
  val topSearch = PathMatcher("topsearch")
}

case class SearchStarsRequest(name: Option[String],
                              country: Option[CountryId],
                              category: Option[StarCategoryId],
                              sort: Option[StarSort],
                              fields: Option[Set[StarField]]) extends Validatable {

  def validate(ctx: ValidationContext) {
    name.map { n =>
      if(n.length < 3)
        ctx.fail("Name must contain more that 2 chars")
    }
    if(name.isEmpty && country.isEmpty && category.isEmpty)
      ctx.fail("Required name, category, region or country")
  }
}

case class StarRegistrationParams(@JsonProperty("regionid") regionId: RegionId,
                            category: StarCategoryId,
                            gender: Gender,
                            name: String,
                            @JsonProperty("lastname") lastName: String,
                            email: String,
                            pwd: String,
                            @JsonProperty("countrycode") countryCode: String,
                            phone: String,
                            media: Option[String]) extends UnmarshallerEntity {
  def registrationPhone = Phone(countryCode, phone)
  def star(adminUserId: AdminUserId) = UserStar(starCategoryIds = Seq(category), None, Option(adminUserId), 0)
}
