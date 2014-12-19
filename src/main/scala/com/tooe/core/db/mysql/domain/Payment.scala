package com.tooe.core.db.mysql.domain

import java.math.{BigInteger, BigDecimal}
import java.sql.Timestamp
import javax.persistence._
import org.hibernate.annotations.Type
import com.tooe.core.util.HashHelper
import org.bson.types.ObjectId
import com.tooe.core.domain.UserId
import scala.beans.{BooleanBeanProperty, BeanProperty}
import java.util.Date

object Payment {
  type OrderId = BigInt
}

@Entity(name="payments")
case class Payment(
  @Column(name = "callback_uuid", nullable = false)
  uuid: String = HashHelper.uuid,

  @Column(name="user_id", nullable=false)
  userId: String = null,

  @Column(name="payment", nullable=false)
  var amount: BigDecimal = null, //FULL AMOUNT

  @Column(name="currency_id", nullable=false)
  currencyId: String = null,

  @Column(name="expire_date", nullable=false)
  expireDate: Timestamp = null

) extends HasOrderId {
  def this() = this(null)

  @Id
  @Column(name = "order_id", nullable = false)
  @GeneratedValue(strategy = GenerationType.AUTO)
  var orderJpaId: BigInteger = _

  @OneToOne(fetch = FetchType.EAGER, cascade = Array(CascadeType.ALL))
  @PrimaryKeyJoinColumn(name = "order_id", referencedColumnName = "order_id")
  var productSnapshot: ProductSnapshot = _

  @OneToOne(fetch = FetchType.EAGER, cascade = Array(CascadeType.ALL))
  @PrimaryKeyJoinColumn(name = "order_id")
  var paymentSystem: PaymentSystem =_

  @OneToOne(fetch = FetchType.EAGER, cascade = Array(CascadeType.ALL))
  @PrimaryKeyJoinColumn(name = "order_id")
  var recipient: Recipient = _

  @Column(name="trans_id", nullable=true)
  var transactionId: String = _

  // Hibernate bug workaround
  // https://hibernate.onjira.com/browse/HHH-6935
  @Type(`type` = "org.hibernate.type.NumericBooleanType")
  @Column(name="rejected", columnDefinition="BIT")
  @BooleanBeanProperty
  var rejected: Boolean = false

  // Hibernate bug workaround
  // https://hibernate.onjira.com/browse/HHH-6935
  @Type(`type` = "org.hibernate.type.NumericBooleanType")
  @Column(name="deleted", columnDefinition="BIT")
  @BooleanBeanProperty
  var deleted: Boolean = false

  @Type(`type` = "org.hibernate.type.NumericBooleanType")
  @Column(name="received", columnDefinition="BIT")
  var isReceived: Boolean = false

  @Column(name="exportcounter")
  @BeanProperty
  var exportCount: Int = 0

  @Column(name="exporttime", nullable=true)
  @BeanProperty
  var exportTime: Timestamp = null

  @Column(name="exportupdatetime", nullable=true)
  @BeanProperty
  var exportUpdateTime: Timestamp = null

  @Column(name="date", nullable=false)
  @BeanProperty
  var date: Timestamp = null

  def isOpen: Boolean = !isFinished
  
  def isFinished: Boolean = isFailed || isSucceeded

  def isFailed: Boolean = isDeleted || isRejected

  def isSucceeded: Boolean = transactionId != null

  def getUserId = UserId(new ObjectId(userId))

  def getExpireDate: Date = new Date(expireDate.getTime)
}