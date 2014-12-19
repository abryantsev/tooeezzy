package com.tooe.core.service

import org.springframework.stereotype.Service
import com.tooe.core.db.mongo.domain.InfoMessage
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.InfoMessageRepository

trait InfoMessageDataService {
  def findOne(id: String): Option[InfoMessage]
}

@Service
class InfoMessageDataServiceImpl extends InfoMessageDataService {
  @Autowired var repo: InfoMessageRepository = _

  def findOne(id: String) = Option(repo.findOne(id))
}