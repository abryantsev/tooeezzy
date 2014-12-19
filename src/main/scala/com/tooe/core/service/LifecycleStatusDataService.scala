package com.tooe.core.service

import com.tooe.core.db.mongo.domain.LifecycleStatus
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.LifecycleStatusRepository

trait LifecycleStatusDataService {

  def findAll: Seq[LifecycleStatus]

  def save(ls: LifecycleStatus): LifecycleStatus

}

@Service
class LifecycleStatusDataServiceImpl extends LifecycleStatusDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: LifecycleStatusRepository = _

  def findAll = repo.findAll()

  def save(ls: LifecycleStatus) = repo.save(ls)
}
