package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.domain.{ExportStatus, AdminRoleId}
import com.tooe.core.usecase.{OrderWriteActor, ExportOrdersCompleteRequest, OrderReadActor}
import akka.pattern._
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.db.mongo.util.UnmarshallerEntity

class OrderExportService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import OrderExportService._

  lazy val orderReadActor = lookup(OrderReadActor.Id)
  lazy val orderWriteActor = lookup(OrderWriteActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
    pathEnd {
      authenticateAdminBySession { implicit adminSession: AdminUserSession =>
        authorizeByRole(AdminRoleId.Exporter) {
          get {
           complete((orderReadActor ? OrderReadActor.GetOrdersFotExport).mapTo[SuccessfulResponse])
          } ~
          post {
            entity(as[ExportOrdersCompleteRequest]) { request: ExportOrdersCompleteRequest =>
              complete((orderWriteActor ? OrderWriteActor.OrdersExportComplete(request)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    }
  }

}

object OrderExportService {
  val Root = "ordersexport"
}
