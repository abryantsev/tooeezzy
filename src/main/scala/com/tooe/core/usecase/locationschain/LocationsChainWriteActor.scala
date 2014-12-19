package com.tooe.core.usecase.locationschain

import com.tooe.api.service.{SuccessfulResponse, ExecutionContextProvider}
import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.domain.{LocationId, LocationsChainId}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.db.mongo.query.UpdateResult

object LocationsChainWriteActor {
  final val Id = Actors.LocationsChainWrite

  case class AddLocationsToChain(locationChainId: LocationsChainId, locations: Seq[LocationId])
}

class LocationsChainWriteActor extends AppActor with ExecutionContextProvider {

  import LocationsChainWriteActor._
  lazy val locationDataActor = lookup(LocationDataActor.Id)

  def receive = {
    case AddLocationsToChain(locationChainId, locations) =>
      locationDataActor.ask(LocationDataActor.AddLocationsToChain(locationChainId, locations)).mapTo[UpdateResult]
        .map(_ => SuccessfulResponse) pipeTo sender
  }
}

case class AddLocationsToChainParams(locations: Seq[LocationId]) extends UnmarshallerEntity
