package com.tooe.core.migration.service

import org.springframework.stereotype.Service
import com.tooe.core.migration.db.domain.MappingCollection.MappingCollection
import com.tooe.core.migration.db.domain.IdMapping
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.migration.db.repository.IdMappingRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.migration.MigrationException
import org.bson.types.ObjectId
import scala.collection.convert.{WrapAsJava, WrapAsScala}

trait IdMappingDataService {

  def find(legacyId: Int, collection: MappingCollection, ownerNewId: Option[ObjectId] = None): IdMapping
  def findMappings(legacyIds: Seq[Int], collection: MappingCollection): Seq[ObjectId]
  def save(idMapping: IdMapping): IdMapping

}

@Service
class IdMappingDataServiceImpl extends IdMappingDataService {

  @Autowired var repo: IdMappingRepository = _
  @Autowired var mongo: MongoTemplate = _

  def find(legacyId: Int, collection: MappingCollection, ownerNewId: Option[ObjectId] = None) = {
    import com.tooe.core.util.BuilderHelper._
    val query = Query.query(Criteria.where("cn").is(collection.toString).and("lid").is(legacyId))
      .applyIfDefined(ownerNewId) {
      (query, oid) => query.addCriteria(Criteria.where("oid").is(oid))
    }
    WrapAsScala.asScalaBuffer(mongo.find(query, classOf[IdMapping])).toList match {
      case x :: Nil => x
      case ms@(x :: xs) => throw new MigrationException(s"Duplicated id mappings for $legacyId are ${ms.mkString("[", ", ", "]")}")
      case Nil => throw new MigrationException(s"Could not find id mappings for $legacyId in ${collection.toString}")
      case _ => throw new Error("dafaq! seriously")
    }

  }

  // toMap makes collection distinct by keys in reverse order, like:
  // List((1, 2), (1, 3), (2, 3)).toMap == Map(1 -> 3, 2 -> 3) that's why...
  def findMappings(legacyIds: Seq[Int], collection: MappingCollection): Seq[ObjectId] = {
    val query = Query.query(Criteria.where("cn").is(collection.toString).and("lid").in(WrapAsJava.asJavaCollection(legacyIds)))
    val idsMap = Option(mongo.find(query, classOf[IdMapping])).map(WrapAsScala.collectionAsScalaIterable(_).groupBy(_.legacyId))
      .getOrElse(throw new MigrationException(s"Could not find id mappings for $legacyIds in ${collection.toString}"))
      .mapValues(
      xs =>
        if (xs.tail.isEmpty) xs.head.newId
        else throw new MigrationException(s"Duplicated id mappings for ${xs.head.legacyId} are ${xs.mkString("[", ", ", "]")}"))
      .withDefault(v => throw new MigrationException(s"Could not find id mapping for $v in ${collection.toString}"))
    legacyIds.map(idsMap)
  }

  def save(idMapping: IdMapping) = repo.save(idMapping)

}