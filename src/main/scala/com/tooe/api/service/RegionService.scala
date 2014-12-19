package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.db.mongo.domain.StatisticFields
import akka.pattern._
import spray.routing.{PathMatcher, Directives}
import com.tooe.core.usecase._
import com.tooe.core.domain.{StarCategoryId, CountryId}

class RegionService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import RegionService._
  import RegionActor._

  lazy val regionActor = lookup(RegionActor.Id)
  val route =
    (mainPrefix & pathPrefix(RegionsPath / PathMatcher("country") / Segment).as(CountryId)) { (routeContext: RouteContext, countryId: CountryId) =>
      pathEndOrSingleSlash {
        get {
          complete(regionActor.ask(GetRegions(countryId, routeContext)).mapTo[Regions])
        }
      } ~
        pathPrefix(Statistics) {
          path(WithLocations) {
            get {
              authenticateBySession { s: UserSession =>
                completeWith(countryId, routeContext, StatisticFields(locations = true))
              }
            }
          } ~
          path(WithPromotions) {
            get {
              authenticateBySession { s: UserSession =>
                completeWith(countryId, routeContext, StatisticFields(promotions = true))
              }
            }
          } ~
          path(WithSales) {
            get {
              authenticateBySession { s: UserSession =>
                completeWith(countryId, routeContext, StatisticFields(sales = true))
              }
            }
          } ~
          path(WithUsers) {
            get {
              authenticateBySession { s: UserSession =>
                completeWith(countryId, routeContext, StatisticFields(users = true))
              }
            }
          } ~
          path(WithFavorites) {
            get {
              authenticateBySession { s: UserSession =>
                completeWith(countryId, routeContext, StatisticFields(favorites = true))
              }
            }
          } ~
          path(WithProducts) {
            get {
              authenticateBySession { s: UserSession =>
                completeWith(countryId, routeContext, StatisticFields(products = true))
              }
            }
          } ~
          path(WithStarCategory / Segment).as(StarCategoryId) { starCategory: StarCategoryId =>
            get {
              authenticateBySession { s: UserSession =>
                completeWith(countryId, routeContext, StatisticFields(starCategory = Some(starCategory)))
              }
            }
          }
        }
      }

  def completeWith(countryId: CountryId, routeContext: RouteContext, statisticFields: StatisticFields) = {
    complete((regionActor ? RegionActor.FindByStatistics(countryId, statisticFields, routeContext)).mapTo[SuccessfulResponse])
  }
}

object RegionService extends Directives{
  val RegionsPath = PathMatcher("regions")
  val Statistics = PathMatcher("statistics")
  val WithLocations = PathMatcher("withlocations")
  val WithPromotions = PathMatcher("withpromotions")
  val WithSales = PathMatcher("withsales")
  val WithUsers = PathMatcher("withusers")
  val WithFavorites = PathMatcher("withfavorites")
  val WithStarCategory = PathMatcher("starscategory")
  val WithProducts = PathMatcher("withproducts")
}