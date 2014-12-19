package com.tooe.core.db.mysql.domain

import javax.persistence.Entity
import org.hibernate.annotations.Type
import java.math.BigInteger
import com.tooe.core.domain.{PhoneShort, UserId}
import org.bson.types.ObjectId

@Entity(name = "recipients")
case class Recipient
(
  @MapsId
  @OneToOne(mappedBy = "productSnapshot")
  @JoinColumn(name = "order_id")
  payment: Payment,

  @Column(name = "recipient_id", nullable = true)
  recipientId: String = null,

  @Column(name = "email", nullable = true)
  email: String = null,

  @Column(name = "phone", nullable = true)
  phone: String = null,

  @Column(name = "country_id", nullable = true)
  countryId: String = null,

  @Column(name = "country_pc", nullable = true)
  phoneCode: String = null,

  // Hibernate bug workaround
  // https://hibernate.onjira.com/browse/HHH-6935
  @Type(`type` = "org.hibernate.type.NumericBooleanType")
  @Column(name = "show_actor", columnDefinition = "BIT")
  showActor: Boolean = false,

  // Hibernate bug workaround
  // https://hibernate.onjira.com/browse/HHH-6935
  @Type(`type` = "org.hibernate.type.NumericBooleanType")
  @Column(name = "isprivate", columnDefinition = "BIT")
  isPrivate: Boolean = false
  ) extends HasOrderId {
  def this() = this(null)

  if (payment != null) payment.recipient = this

  @Id
  @Column(name = "order_id", nullable = false)
  var orderJpaId: BigInteger = _

  def getRecipientId: Option[UserId] = Option(recipientId) map { str => UserId(new ObjectId(str)) }

  def hideSender: Option[Boolean] = if (showActor) None else Some(true)

  def getEmail: Option[String] = Option(email)

  def getPhoneCode: Option[String] = Option(phoneCode)

  def getPhone: Option[String] = Option(phone)

  def getPhoneShort: Option[PhoneShort] = for {
    code <- getPhoneCode
    number <- getPhone
  } yield PhoneShort(code = code, number = number)
}