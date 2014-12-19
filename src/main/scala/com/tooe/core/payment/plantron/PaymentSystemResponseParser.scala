package com.tooe.core.payment.plantron

import scala.xml.XML
import com.tooe.api.JsonProp
import com.tooe.api.service.SuccessfulResponse
import com.tooe.core.domain.MediaUrl

object PaymentSystemResponseParser {

  import com.tooe.core.util.Images._
  import com.tooe.core.util.Images.PaymentSystems.Image
  import com.tooe.core.util.MediaHelper._

  def parse(rawXml: String): PaymentSystemResponse = {

    val xml = XML.loadString(rawXml.replace("""<?xml version="1.0" encoding="UTF-8"?>""", ""))

    val systems = xml \ "pg_payment_system" map { sysNode =>
      val subSystems = sysNode \ "pg_sub_payment_systems" \ "pg_sub_payment_system" map { subNode =>
        val name = (subNode \ "pg_sub_name").text
        SubSystem(
          name = name,
          description = (subNode \ "pg_sub_description").text,
          Option(staticMediaUrl(Image.getImage(name), PaymentSystems.Full.Media))
        )
      }
      def requiredFields = (sysNode \\ "pg_required").map(_.text) match {
        case Nil => None
        case xs  => Some(xs)
      }
      def currencyCode =
        (sysNode \ "pg_amount_to_pay_currency").text match {
          case "руб." => "RUR"
          case v      => v
        }
      val name = (sysNode \ "pg_name").text
      PaymentSystem(
        name = name,
        description = (sysNode \ "pg_description").text,
        media = staticMediaUrl(Image.getImage(name), PaymentSystems.Full.Media),
        scenario = (sysNode \ "pg_payment_scenario").text,
        amount = BigDecimal((sysNode \ "pg_amount_to_pay").text),
        currencyCode = currencyCode,
        required = requiredFields,
        subSystems = if (subSystems.isEmpty) None else Some(subSystems)
      )
    }
    PaymentSystemResponse(systems)
  }
}

case class PaymentSystemResponse(@JsonProp("paymentsystems") systems: Seq[PaymentSystem]) extends SuccessfulResponse

case class PaymentSystem
(
  name: String,
  description: String,
  media: MediaUrl,
  @JsonProp("payment_scenario") scenario: String,
  @JsonProp("amount_to_pay") amount: BigDecimal,
  @JsonProp("amount_to_pay_currency") currencyCode: String,
  @JsonProp("required") required: Option[Seq[String]],
  @JsonProp("sub_payment_systems") subSystems: Option[Seq[SubSystem]]
  )

case class SubSystem
(
  name: String,
  description: String,
  media: Option[MediaUrl]
  )