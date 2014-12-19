package com.tooe.core.migration.service

import com.tooe.core.migration.db.domain.MappingDictionary.MappingDictionary
import com.tooe.core.migration.db.domain.DictionaryIdMapping
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.migration.db.repository.DictionaryIdMappingRepository
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.migration.MigrationException

trait DictionaryIdMappingDataService {

  def find(legacyId: Int, dictionary: MappingDictionary): DictionaryIdMapping
}

@Service
class DictionaryIdMappingDataServiceImpl extends DictionaryIdMappingDataService {

  @Autowired var repo: DictionaryIdMappingRepository = _
  @Autowired var mongo: MongoTemplate = _

  def find(legacyId: Int, dictionary: MappingDictionary): DictionaryIdMapping = {
    val query = Query.query(Criteria.where("dn").is(dictionary.toString).and("lid").is(legacyId))
    Option(mongo.findOne(query, classOf[DictionaryIdMapping])) getOrElse (throw new MigrationException(s"Legacy ID of $legacyId not found in ${dictionary.toString}"))
  }
}