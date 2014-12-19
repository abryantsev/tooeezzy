package com.tooe.core.db.mysql.domain

import java.math.BigInteger
import javax.persistence.Transient

trait HasOrderId {
  def orderJpaId: BigInteger

  @Transient
  def orderId: Payment.OrderId = BigInt(orderJpaId)
}
