package com.tooe.api.service

import spray.routing.PathMatcher
import akka.actor.ActorSystem
import akka.pattern._
import com.tooe.core.domain.{AdminRoleId, ExportStatus, CompanyId}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.{CompanyWriteActor, CompanyReadActor}
import com.tooe.core.usecase.CompanyReadActor.GetExportedCompanies
import com.tooe.core.usecase.CompanyWriteActor.CompaniesExported
import com.tooe.core.db.mongo.domain.AdminRole

class CompaniesExportService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import CompaniesExportService._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val companyReadActor = lookup(CompanyReadActor.Id)
  lazy val companyWriteActor = lookup(CompanyWriteActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
    authenticateAdminBySession { implicit s: AdminUserSession =>
      authorizeByRole(AdminRoleId.Exporter) {
        pathEndOrSingleSlash {
          get {
            complete((companyReadActor ? GetExportedCompanies).mapTo[SuccessfulResponse])
          } ~
          post {
            entity(as[CompanyExportedRequest]) { request: CompanyExportedRequest =>
              complete((companyWriteActor ? CompaniesExported(request)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    }
  }

}

object CompaniesExportService {
  val Root = PathMatcher("companiesexport")
}

case class CompanyExportedRequest(ids: Seq[CompanyId], status: ExportStatus) extends UnmarshallerEntity
