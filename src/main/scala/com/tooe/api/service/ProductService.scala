package com.tooe.api.service

import akka.actor.ActorSystem
import spray.routing.{PathMatcher, Directives}
import akka.pattern.ask
import shapeless.HNil
import com.tooe.core.domain._
import spray.http.StatusCodes
import com.tooe.core.usecase.product._
import com.tooe.core.usecase._

class ProductService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  import ProductService._
  import ProductReadActor._
  import ProductWriteActor._

  lazy val productReadActor = lookup(ProductReadActor.Id)
  lazy val productWriteActor = lookup(ProductWriteActor.Id)

  val entities = parameters('entities.as[CSV[ProductSearchView]] ?)

  val productAdminSearchOffset = getOffsetLimit()

  implicit def csvToSeq[T](cvs: CSV[T]) = cvs.toSeq

  val route =
    (mainPrefix & path(ProductsPath / "search")) { (routeContext: RouteContext) =>
      (get & authenticateBySession) { _: UserSession =>
        ((parameter(paramListSearchByRegion) & entities).as(ProductSearchRequest) & parameters('region.as[RegionId])) { (request: ProductSearchRequest, regionId: RegionId) =>
          offsetLimit { offsetLimit: OffsetLimit =>
            complete((productReadActor ? GetProductsSearch(request, regionId, offsetLimit, routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    (mainPrefix & path(ProductsPath / "location" / Segment / "search").as(LocationId)) { (routeContext: RouteContext, locationId: LocationId) =>
      (get & authenticateBySession) { _: UserSession =>
        ((parameter(paramListSearchByLocation) & entities).as(LocationProductSearchRequest) & parameters('ptype.as[ProductTypeId] ?)) { (request: LocationProductSearchRequest, productType:Option[ProductTypeId]) =>
          offsetLimit { offsetLimit: OffsetLimit =>
            complete((productReadActor ? GetProductsLocationSearch(request, locationId, productType, offsetLimit, routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    (mainPrefix & path(ProductsPath / ObjectId).as(ProductId)) { (routeContext: RouteContext, productId: ProductId) =>
      get {
        parameter('view.as[ProductView] ?) { viewOpt: Option[ProductView] => {
            val view = viewOpt.getOrElse(ProductView.Default)
            if (view == ProductView.Admin)
              authenticateAdminBySession { implicit s: AdminUserSession =>
                authorizeByRole(AdminRoleId.Client) {
                  complete((productReadActor ? GetProductInfo(productId, None, view, routeContext)).mapTo[SuccessfulResponse])
                }
              }
            else
              tryAuthenticateRegularUser { userSession: UserSession =>
                complete((productReadActor ? GetProductInfo(productId, Some(userSession.userId), view, routeContext)).mapTo[SuccessfulResponse])
              }
          }
        }
      } ~
      (post & authenticateAdminBySession) { implicit s: AdminUserSession =>
        entity(as[ProductChangeRequest]) { request: ProductChangeRequest =>
          (authorizeByRole(AdminRoleId.Client) & authorizeByResource(productId)) {
            complete((productWriteActor ? ProductWriteActor.ChangeProduct(productId, request, routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      } ~
      (delete & authenticateAdminBySession) { implicit s: AdminUserSession =>
        (authorizeByRole(AdminRoleId.Client) & authorizeByResource(productId)) {
            complete((productWriteActor ? ProductWriteActor.DeleteProduct(productId)).mapTo[SuccessfulResponse])
          }
      }
    } ~
    (mainPrefix & path(ProductsPath / Segment / "details").as(ProductId)) { (routeContext: RouteContext, productId: ProductId) =>
      (get & authenticateBySession) { _: UserSession =>
        complete(productReadActor.ask(GetProductMoreInfo(productId, routeContext)).mapTo[ProductMoreInfoResponse])
      }
    } ~
    (mainPrefix & path(ProductsPath)) { routeContext: RouteContext =>
      (post & authenticateAdminBySession) { implicit s: AdminUserSession =>
        authorizeByRole(AdminRoleId.Client) {
          entity(as[SaveProductRequest]) { ( sp:SaveProductRequest) =>
            authorizeByResource(sp.locationId) {
            complete(StatusCodes.Created,
              productWriteActor.ask(SaveProduct(sp, s.adminUserId, routeContext)).mapTo[ProductIdResponse])
            }
          }
        }
      }
    } ~
    (mainPrefix & path(ProductCategoriesPath)) { routeContext: RouteContext =>
      (get & authenticateAnyUser) { _ =>
        parameter('region.as[RegionId] ?) { reg: Option[RegionId] =>
          complete(productReadActor.ask(GetProductCategories(reg, routeContext)).mapTo[LocationCategoriesResponse])
        }
      }
    } ~
    (mainPrefix & path(ProductsPath / CompanyPath / Segment / "admsearch").as(CompanyId)) { (routeContext: RouteContext, companyId: CompanyId) =>
      (get & authenticateAdminBySession) { implicit s: AdminUserSession =>
        (authorizeByRole(AdminRoleId.Client) & authorizeByResource(companyId) ) {
          parameters('name.as[String] ?, 'location.as[LocationId] ?, 'ptype.as[ProductTypeId] ?, 'sort.as[ProductAdminSearchSortType] ?) {
            (name: Option[String], locationId: Option[LocationId], productType: Option[ProductTypeId], sort: Option[ProductAdminSearchSortType]) =>
              productAdminSearchOffset { offsetLimit: OffsetLimit =>
                complete((productReadActor ? GetProductsAdminSearch(ProductAdminSearchRequest(companyId, locationId, productType, name, sort), offsetLimit, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    }

  val commonSearchFields = ('name ?) :: ('pcurrency ?) :: ('pmin ?) :: ('pmax ?) :: ('issale ?) :: ('sort ? "name") :: HNil

  val paramListSearchByRegion = ('category ?) :: commonSearchFields

  val paramListSearchByLocation = ('productcategory ?) :: commonSearchFields


}

object ProductService extends Directives{
  val ProductPath = PathMatcher("product")
  val CompanyPath = PathMatcher("company")
  val ProductsPath = PathMatcher("products")
  val ProductCategoriesPath = PathMatcher("productcategories")
}