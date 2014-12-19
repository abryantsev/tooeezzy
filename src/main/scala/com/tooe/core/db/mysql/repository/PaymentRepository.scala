package com.tooe.core.db.mysql.repository

import org.springframework.data.jpa.repository.{Modifying, JpaRepository, Query}
import org.springframework.transaction.annotation.Transactional
import com.tooe.core.db.mysql.domain.Payment
import java.math.BigInteger
import org.springframework.data.repository.query.Param
import java.util.Date
import org.springframework.data.domain.Pageable

@Transactional(readOnly=true)
trait PaymentRepository extends JpaRepository[Payment, BigInteger] {
	
  @Query("select p from payments p inner join fetch p.productSnapshot")
  def findAllWithJoin(): java.util.List[Payment]

  def findByUuid(uuid: String): java.util.List[Payment]
  
  @Query("""
    select count(p) from payments p inner join p.productSnapshot ps
    where ps.productId = :productId and p.rejected <> true and p.transactionId is null and p.deleted <> true
  """)
  def countOpenPayments(@Param("productId") productId: String): Long

  @Modifying
  @Query("""
    update payments set deleted = true
    where deleted <> true and expireDate <= :runDate and rejected <> true and transactionId is null
  """)
  def markDeleteOverduePayments(@Param("runDate") runDate: Date): Int

  @Modifying
  @Query("""update payments set expireDate = :expireDate where order_id = :orderId and deleted <> true""")
  def updateExpireDate(@Param("orderId") orderId: BigInteger, @Param("expireDate") expireDate: Date): Int

  @Query("""select p from payments p fetch all properties
   where p.deleted = false and p.rejected = false and (p.exportTime is null or p.exportTime < :now) and p.exportUpdateTime is null and p.exportCount < 5
   order by p.date""")
  def getForExport(@Param("now") now: Date, pageable: Pageable): java.util.List[Payment]

  @Query("""select count(p) from payments p
          where p.deleted = false and p.rejected = false and (p.exportTime is null or p.exportTime < :now) and p.exportUpdateTime is null and p.exportCount < 5""")
  def getForExportCount(@Param("now") now: Date): Long

  @Modifying
  @Query("""update payments set exportTime = :now, exportCount = exportCount + 1 where orderJpaId in :ids""")
  def markAsTryExport(@Param("now") now: Date, @Param("ids") ids: java.util.List[BigInteger]): Int

  @Modifying
  @Query("""update payments set exportUpdateTime = :date where orderJpaId in :ids""")
  def markOrderExportComplete(@Param("ids")ids: java.util.List[BigInteger], @Param("date")date: Date): Unit
}