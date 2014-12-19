package com.tooe.core.service

import com.tooe.core.db.mongo.domain.AdminRole
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.AdminRoleRepository

trait AdminRoleDataService {

  def findAll: Seq[AdminRole]

  def save(ar: AdminRole): AdminRole

}

@Service
class AdminRoleDataServiceImpl extends AdminRoleDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: AdminRoleRepository = _

  def findAll = repo.findAll()

  def save(ar: AdminRole) = repo.save(ar)
}
