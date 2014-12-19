package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.db.mysql.services.ProductSnapshotDataService
import java.math.BigInteger
import scala.concurrent.Future

object ProductSnapshotDataActor {
  final val Id = Actors.ProductSnapshotData

  case class GetProductSnapshot(id: BigInteger)
  case class GetProductSnapshots(ids: Seq[BigInteger])

}

class ProductSnapshotDataActor extends AppActor {
  import scala.concurrent.ExecutionContext.Implicits.global
  import ProductSnapshotDataActor._

  lazy val service = BeanLookup[ProductSnapshotDataService]

  def receive = {
    case GetProductSnapshot(id) => Future { service.find(id).getOrNotFound(id, "ProductSnapshot not found") } pipeTo sender
    case GetProductSnapshots(ids) => Future(service.findAllByIds(ids)).pipeTo(sender)
  }

}
