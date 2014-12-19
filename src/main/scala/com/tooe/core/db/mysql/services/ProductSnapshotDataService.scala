package com.tooe.core.db.mysql.services

import com.tooe.core.db.mysql.domain.{Payment, ProductSnapshot}
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mysql.repository.ProductSnapshotRepository
import java.math.BigInteger
import scala.collection.JavaConverters._

trait ProductSnapshotDataService {

  def save(product: ProductSnapshot): ProductSnapshot
  def find(id: BigInteger): Option[ProductSnapshot]
  def findAllByIds(ids: Seq[BigInteger]): Seq[ProductSnapshot]

}

@Service
@Transactional(readOnly=true)
class ProductSnapshotDataServiceImpl extends ProductSnapshotDataService {

  import scala.collection.JavaConversions._

  @Autowired var repository: ProductSnapshotRepository = _

  def save(product: ProductSnapshot) = repository.saveAndFlush(product)

  def find(id: BigInteger) = Option(repository.findOne(id))

  def findAllByIds(ids: Seq[BigInteger]) = repository.findAll(ids).asScala.toSeq

}
