package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.MaritalStatus

trait MaritalStatusRepository extends MongoRepository[MaritalStatus, String] {

  @Query("{ id: { $in : ?0 } }")
  def findByIds(ids: Seq[String]): java.util.List[MaritalStatus]

  @Query("{ nf: {}, n: { $ne:{} } }")
  def maleStatuses: java.util.List[MaritalStatus]

  @Query("{ n: {}, nf: { $ne:{} } }")
  def femaleStatuses: java.util.List[MaritalStatus]
}