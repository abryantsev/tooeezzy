package com.tooe.core.service

import com.tooe.core.db.mongo.domain.UsersGroup
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.UsersGroupRepository

trait UsersGroupDataService {

  def findAll: Seq[UsersGroup]

  def save(ug: UsersGroup): UsersGroup

}

@Service
class UsersGroupDataServiceImpl extends UsersGroupDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: UsersGroupRepository = _

  def findAll = repo.findAll()

  def save(ug: UsersGroup) = repo.save(ug)
}
