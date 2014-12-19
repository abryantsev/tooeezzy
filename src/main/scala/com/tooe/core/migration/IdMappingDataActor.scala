package com.tooe.core.migration

import akka.pattern._
import com.tooe.core.usecase.AppActor
import com.tooe.core.migration.service.IdMappingDataService
import com.tooe.core.infrastructure.BeanLookup
import scala.concurrent.Future
import com.tooe.core.migration.db.domain.MappingCollection.MappingCollection
import com.tooe.core.migration.db.domain.IdMapping
import org.bson.types.ObjectId
import akka.actor.ActorRef

object IdMappingDataActor {
  final val Id = 'idMappingData

  case class GetIdMapping(legacyId: Int, collection: MappingCollection, ownerNewId: Option[ObjectId] = None)
  case class GetSeqMappings(ids: Seq[Int], collection: MappingCollection)
  case class SaveIdMapping(idMapping: IdMapping)
}



class IdMappingDataActor extends AppActor {
  import IdMappingDataActor._

  lazy val service = BeanLookup[IdMappingDataService]

  def receive = {
    case GetSeqMappings(ids, collection) => Future(service.findMappings(ids, collection)).pipeTo(sender)
    case GetIdMapping(legacyId, collection, ownerNewId) => Future(service.find(legacyId, collection, ownerNewId)).pipeTo(sender)
    case SaveIdMapping(idMapping) => Future(service.save(idMapping)).pipeTo(sender)
  }
}


