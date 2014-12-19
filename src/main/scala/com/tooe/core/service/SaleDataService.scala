package com.tooe.core.service

import com.tooe.core.db.mongo.domain.Sale
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.SaleRepository

trait SaleDataService extends DataService[Sale, ObjectId] {
}

@Service
class SaleDataServiceImpl extends SaleDataService with DataServiceImpl[Sale, ObjectId] {
  @Autowired var repo: SaleRepository = _
}