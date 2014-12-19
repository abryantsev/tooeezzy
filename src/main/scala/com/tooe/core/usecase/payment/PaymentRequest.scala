package com.tooe.core.usecase.payment

import com.tooe.api.JsonProp
import com.tooe.core.db.mongo.util.{HasIdentityFactory, HasIdentity, UnmarshallerEntity}
import com.tooe.core.payment.plantron._
import com.tooe.core.domain.ProductId
import com.tooe.api.validation.Validatable
import com.tooe.api.validation.ValidationContext
import com.tooe.core.domain.UserId
import com.tooe.core.domain.CountryId
import com.tooe.core.usecase.payment.alfabank.AlfabankHttpClientActor.PageView

case class PaymentRequest
(
  @JsonProp("productid") productId: ProductId,
  @JsonProp("recipient") recipient: Recipient,
  msg: String,
  @JsonProp("private") isPrivate: Option[Boolean] = None,
  amount: Amount,
  @JsonProp("paymentsystem") paymentSystem: PaymentSystem,
  @JsonProp("responsetype") responseType: ResponseType, //TODO should present only in rest service layer
  @JsonProp("pageview") pageViewType: Option[String] = None,
  @JsonProp("hideactor") hideActor: Option[Boolean] = None
  ) extends UnmarshallerEntity with Validatable
{
  def validate(ctx: ValidationContext) = {

    val isTooeezzy = paymentSystem.system == PaymentSystem.Tooeezzy
    val isZeroPrice = amount.value == BigDecimal(0.00)

    if (isTooeezzy && !isZeroPrice)
      ctx.fail(s"Only zero prices allowed, when selected payment system is ${PaymentSystem.Tooeezzy}")

    if (!isTooeezzy && isZeroPrice)
      ctx.fail(s"Zero prices are allowed, only when selected payment system is ${PaymentSystem.Tooeezzy}")

    if(pageViewType.exists(_ != "mobile"))
      ctx.fail(s"Wrong pageview parameter")
  }
}

/**
 * can't be put inside PaymentRequest object since Jackson can't work with such types
 * use PaymentRequest.ResponseType everywhere but in the entity declaration
 */
sealed trait ResponseType extends HasIdentity {
  def id: String
}

object ResponseType extends HasIdentityFactory[ResponseType] {

  object JSON extends ResponseType {
    def id = "JSON"
  }

  object HTML extends ResponseType {
    def id = "HTML"
  }

  val values = Seq(JSON, HTML)
  private val idToVal = values.map(v => v.id -> v).toMap

  def get(id: String) = idToVal.get(id)
}

case class Recipient
(
  id: Option[UserId] = None,
  email: Option[String] = None,
  phone: Option[String] = None,
  country: Option[CountryParams] = None
  ) extends UnmarshallerEntity with Validatable {
  
  def validate(ctx: ValidationContext) = {
    val fields =
      (if (id.isDefined) Seq("id") else Nil) ++
      (if (email.isDefined) Seq("email") else Nil) ++
      (if (phone.isDefined || country.isDefined) Seq("phone") else Nil)
    
    if (fields.size == 0) ctx.fail("One of recipient.{id, email, phone} must be specified")
    if (fields.size > 1) ctx.fail("Only one of recipient.{id, email, phone} must be specified")
    if (phone.isDefined ^ country.isDefined) ctx.fail("Both recipient.{phone, country} must be specified together")
  }
}

case class PaymentSystem
(
  system: String,
  subsystem: Option[String] = None,
  @JsonProp("required") requiredFields: Option[RequiredFields] = None
  ) {
  PaymentSystem.checkParams(this)
}

object PaymentSystem {
  val Tooeezzy = "TOOEEZZY"

  def paymentSystemUserIdKey(ps: PaymentSystem): Option[Symbol] = ps.system match {
    //TODO should be configurable via config file
    case "ALFACLICK" => Some('pg_alfaclick_client_id)
    case "MONEYMAILRU" => Some('pg_user_email)
    case _ => None
  }

  def paymentSystemUserId(ps: PaymentSystem): Option[(Symbol, String)] =
    for {
      k <- paymentSystemUserIdKey(ps)
      fields <- ps.requiredFields
      v <- fields.paymentSystemUserId
    } yield k -> v

  def additionalParams(ps: PaymentSystem): PlatronParams = paymentSystemUserId(ps).toSeq

  def checkParams(ps: PaymentSystem) {
    if (paymentSystemUserIdKey(ps).isDefined && paymentSystemUserId(ps).isEmpty) {
      throw new IllegalArgumentException("required field paymentsystem.required.paymentsystemid is not defined")
    }
  }
}

case class RequiredFields(@JsonProp("paymentsystemid") paymentSystemUserId: Option[String])

case class CountryParams(id: CountryId, phone: String) extends UnmarshallerEntity

case class Amount(value: BigDecimal, currency: String) extends UnmarshallerEntity