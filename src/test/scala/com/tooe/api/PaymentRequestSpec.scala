package com.tooe.api

import org.specs2.mutable.SpecificationWithJUnit
import org.bson.types.ObjectId
import com.tooe.core.usecase._
import com.tooe.core.domain.ProductId
import com.tooe.core.usecase.payment.alfabank.AlfabankHttpClientActor.PageView

class PaymentRequestSpec extends SpecificationWithJUnit with MarshalingTestHelper {

  import com.tooe.core.util.SomeWrapper._
  import payment._

  val entity = payment.PaymentRequest(
    productId = ProductId(new ObjectId("512f5dec238ccf0d0cb86fb8")),
    recipient = new Recipient(),
    msg = "3",
    isPrivate = None,
    amount = Amount(BigDecimal(100.98), "RUR"),
    paymentSystem = PaymentSystem(
      system = "CASH",
      requiredFields = RequiredFields(
        paymentSystemUserId = "some user id"
      )
    ),
    responseType = payment.ResponseType.JSON,
    pageViewType = Some("mobile"),
    hideActor = true
  )

  val json = s"""{"productid":"512f5dec238ccf0d0cb86fb8","recipient":{},"msg":"3","amount":{"value":100.98,"currency":"RUR"},"paymentsystem":{"system":"CASH","required":{"paymentsystemid":"some user id"}},"responsetype":"JSON","pageview":"mobile","hideactor":true}"""

  "PaymentParams" should {
    "be deserializable" >> {
      deserialize[payment.PaymentRequest](json) === entity
    }
    "be serializable" >> {
      entityToString(entity) === json
    }
  }
}