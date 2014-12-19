package com.tooe.core.db.mysql.domain

import javax.persistence._
import java.math.BigInteger

@Entity(name = "paym_systems")
case class PaymentSystem
(
  @MapsId
  @OneToOne(mappedBy = "productSnapshot")
  @JoinColumn(name = "order_id")
  payment: Payment,

  @Column(name = "paym_system", nullable = false)
  system: String = null,

  @Column(name = "sub_paym_system", nullable = true)
  subSystem: String = null
  ) extends HasOrderId
{
  def this() = this(null)

  if (payment != null) payment.paymentSystem = this

  @Id
  @Column(name = "order_id", nullable = false)
  var orderJpaId: BigInteger = _
}