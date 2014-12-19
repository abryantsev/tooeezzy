package com.tooe.core.usecase.locationschain

import com.tooe.api.service.{SuccessfulResponse, ExecutionContextProvider}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.{LocationsChainStatsDataService, LocationsChainDataService}
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain.{Location, LocationsChainStats, LocationsChain}
import scala.concurrent.Future
import com.tooe.core.domain.{Coordinates, LocationsChainId}
import com.tooe.core.usecase._
import com.tooe.core.usecase.locationschain.LocationsChainReadActor.GetChainRegions
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object LocationsChainDataActor {

  final val Id = Actors.LocationsChainData

  case class SaveLocationsChain(entity: LocationsChain)
  case class FindLocationsChain(id: LocationsChainId)
  case class FindLocationsChains(ids: Seq[LocationsChainId])
  case class RemoveLocationFormChain(location: Location)
  case class IncreaseLocationsCounter(chainId: LocationsChainId, count: Int)

}

class LocationsChainDataActor extends AppActor with ExecutionContextProvider {

  lazy val service = BeanLookup[LocationsChainDataService]

  import LocationsChainDataActor._

  def receive = {
    case SaveLocationsChain(entity) => Future(service.save(entity)) pipeTo sender
    case FindLocationsChain(id) => Future { service.findOne(id).getOrNotFound(id, s"LocationsChain with ${id.id} not found") } pipeTo sender
    case FindLocationsChains(ids) => Future { service.findAllById(ids)} pipeTo sender
    case IncreaseLocationsCounter(chain, count) => Future { service.changeLocationCounter(chain, count) }

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(LocationsChainId(msg.url.entityId), msg.url.mediaId, msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(LocationsChainId(msg.url.entityId), msg.url.mediaId) }
  }
}