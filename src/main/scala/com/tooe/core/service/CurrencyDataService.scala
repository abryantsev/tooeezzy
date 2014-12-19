package com.tooe.core.service

import com.tooe.core.db.mongo.domain.Currency
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.CurrencyRepository

trait CurrencyDataService {

  def findAll: Seq[Currency]

  def save(c: Currency): Currency

}

@Service
class CurrencyDataServiceImpl extends CurrencyDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: CurrencyRepository = _

  def findAll = repo.findAll()

  def save(c: Currency) = repo.save(c)

}
