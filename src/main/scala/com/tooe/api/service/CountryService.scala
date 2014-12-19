package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.db.mongo.domain.StatisticFields
import akka.pattern.ask
import com.tooe.core.usecase._
import spray.routing.PathMatcher
import com.tooe.core.domain.{CountryField, StarCategoryId}

class CountryService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import CountryService._

  lazy val countryReadActor = lookup(CountryReadActor.Id)

  val fields = parameters('fields.as[CSV[CountryField]] ?)

  val route =
    (mainPrefix & pathPrefix(CountriesPath)) { routeContext: RouteContext =>
      pathEndOrSingleSlash {
        (get & fields) { fields: Option[CSV[CountryField]] =>
           complete(countryReadActor.ask(CountryReadActor.GetCountries(fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
        }
      } ~
      pathPrefix(Phone) {
        path(Segment) { phone: String =>
          (get & fields) { fields: Option[CSV[CountryField]] =>
            authenticateBySession { s: UserSession =>
              complete((countryReadActor ? CountryReadActor.GetCountryByPhone(phone, fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
            }
          }
        }
      } ~
      path(IsActive) {
        (get & fields) { fields: Option[CSV[CountryField]] =>
          authenticateBySession { s: UserSession =>
            complete((countryReadActor ? CountryReadActor.GetActiveCountry(fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
          }
        }
      } ~
      pathPrefix(Statistics) {
        path(WithLocations) {
          (get & fields) { fields: Option[CSV[CountryField]] =>
            authenticateBySession { s: UserSession =>
              complete((countryReadActor ? CountryReadActor.FindByStatistics(StatisticFields(locations = true), fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
            }
          }
        } ~
        path(WithPromotions) {
          (get & fields) { fields: Option[CSV[CountryField]] =>
            authenticateBySession { s: UserSession =>
              complete((countryReadActor ? CountryReadActor.FindByStatistics(StatisticFields(promotions = true), fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
            }
          }
        } ~
        path(WithSales) {
          (get & fields) { fields: Option[CSV[CountryField]] =>
            authenticateBySession { s: UserSession =>
              complete((countryReadActor ? CountryReadActor.FindByStatistics(StatisticFields(sales = true), fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
            }
          }
        } ~
        path(WithUsers) {
          (get & fields) { fields: Option[CSV[CountryField]] =>
            authenticateBySession { s: UserSession =>
              complete((countryReadActor ? CountryReadActor.FindByStatistics(StatisticFields(users = true), fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
            }
          }
        } ~
        path(WithFavorites) {
          (get & fields) { fields: Option[CSV[CountryField]] =>
            authenticateBySession { s: UserSession =>
              complete((countryReadActor ? CountryReadActor.FindByStatistics(StatisticFields(favorites = true), fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
            }
          }
        } ~
        path(WithStarCategory / Segment).as(StarCategoryId) { starCategory: StarCategoryId =>
          (get & fields) { fields: Option[CSV[CountryField]] =>
            authenticateBySession { s: UserSession =>
              complete((countryReadActor ? CountryReadActor.FindByStatistics(StatisticFields(starCategory = Some(starCategory)), fields.map(_.toSet).getOrElse(CountryField.values.toSet), routeContext)).mapTo[Countries])
            }
          }
        }
      }
    }
}

case class CountryExtractor(id: Option[String])

object CountryService  {
  val CountryPath = PathMatcher("country")
  val CountriesPath = PathMatcher("countries")
  val Phone = PathMatcher("phone")
  val IsActive = PathMatcher("isactive")
  val Statistics = PathMatcher("statistics")
  val WithLocations = PathMatcher("withlocations")
  val WithPromotions = PathMatcher("withpromotions")
  val WithSales = PathMatcher("withsales")
  val WithUsers = PathMatcher("withusers")
  val WithFavorites = PathMatcher("withfavorites")
  val WithStarCategory = PathMatcher("starscategory")
}