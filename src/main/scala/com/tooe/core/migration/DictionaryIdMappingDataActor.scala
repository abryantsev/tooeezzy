package com.tooe.core.migration

import akka.pattern._
import com.tooe.core.migration.db.domain.MappingDictionary.MappingDictionary
import com.tooe.core.usecase.AppActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.migration.service.DictionaryIdMappingDataService
import scala.concurrent.Future

object DictionaryIdMappingDataActor {

  final val Id = 'dictionaryIdMappingData

  case class GetDictionaryIdMapping(legacyId: Int, dictionary: MappingDictionary)

}

class DictionaryIdMappingDataActor extends AppActor {

  import DictionaryIdMappingDataActor._

  lazy val service = BeanLookup[DictionaryIdMappingDataService]

  def receive = {
    case GetDictionaryIdMapping(legacyId, dictionary) => Future(service.find(legacyId, dictionary)).pipeTo(sender)
  }
}