package com.tooe.core.service

import com.tooe.core.db.mongo.domain.MaritalStatus
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.MaritalStatusRepository
import com.tooe.core.domain.MaritalStatusId
import scala.collection.JavaConverters._

trait MaritalStatusDataService {
  def save(entity: MaritalStatus): MaritalStatus

  def findOne(id: MaritalStatusId): Option[MaritalStatus]

  def find(ids: Set[MaritalStatusId]): Seq[MaritalStatus]

  def all: Seq[MaritalStatus]

}

@Service
class MaritalStatusDataServiceImpl extends MaritalStatusDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: MaritalStatusRepository = _

  def save(entity: MaritalStatus) = repo.save(entity)

  def findOne(id: MaritalStatusId) = Option(repo.findOne(id.id))

  def find(ids: Set[MaritalStatusId]) = repo.findByIds(ids.toSeq.map(_.id)).asScala

  def all = repo.findAll.asScala
}
