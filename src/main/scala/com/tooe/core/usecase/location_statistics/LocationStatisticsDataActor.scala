package com.tooe.core.usecase.location_statistics

import akka.pattern._
import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.domain.LocationStatisticsId
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.LocationStatisticsDataService
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.LocationStatistics

object LocationStatisticsDataActor {

  final val Id = Actors.LocationStatisticsDataActor

  case class FindLocationStatistics(id: LocationStatisticsId)

  case class SaveLocationStatistics(locationStatistics: LocationStatistics)
}

class LocationStatisticsDataActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationStatisticsDataActor._

  lazy val service = BeanLookup[LocationStatisticsDataService]

  def receive = {
    case FindLocationStatistics(id) => Future { service.findOne(id) } pipeTo sender
    case SaveLocationStatistics(statistics) => Future {service.save(statistics)} pipeTo sender
  }
}
