package com.tooe.core.service

import com.tooe.core.db.mongo.domain.ModerationStatus
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.ModerationStatusRepository

trait ModerationStatusDataService {

  def findAll: Seq[ModerationStatus]

  def save(md: ModerationStatus): ModerationStatus
  
}

@Service
class ModerationStatusDataServiceImpl extends ModerationStatusDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: ModerationStatusRepository = _

  def findAll = repo.findAll()

  def save(md: ModerationStatus) = repo.save(md)

}
