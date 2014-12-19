package com.tooe.core.service

import org.springframework.data.mongodb.repository.MongoRepository
import scala.collection.JavaConverters._

@deprecated
trait DataService[Entity, Id] {

  def save(entity: Entity): Entity

  def findOne(id: Id): Option[Entity]

  def findAll: Seq[Entity]
}

@deprecated
trait DataServiceImpl[Entity, Id <: java.io.Serializable] extends DataService[Entity, Id] {
  def repo: MongoRepository[Entity, Id]

  def save(entity: Entity) = repo.save(entity)

  def findOne(id: Id) = Option(repo.findOne(id))

  def findAll = repo.findAll.asScala
}