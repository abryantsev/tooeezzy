package com.tooe.core.service

import org.springframework.stereotype.Service
import com.tooe.core.db.mongo.repository.EventGroupRepository
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.domain.EventGroup

trait EventGroupDataService {

  def findAll: Seq[EventGroup]

  def save(eg: EventGroup): EventGroup

}

@Service
class EventGroupDataServiceImpl extends EventGroupDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: EventGroupRepository = _

  def findAll = repo.findAll()

  def save(eg: EventGroup) = repo.save(eg)

}
