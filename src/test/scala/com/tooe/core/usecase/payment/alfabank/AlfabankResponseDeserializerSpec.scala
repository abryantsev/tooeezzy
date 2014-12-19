package com.tooe.core.usecase.payment.alfabank

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers

class AlfabankResponseDeserializerSpec extends SpecificationWithJUnit with MustMatchers {

  import com.tooe.core.util.SomeWrapper._

  "AlfabankResponseDeserializer" should {
    "deserialize" >> {
      val deserializer: AlfabankResponseDeserializer = AlfabankResponseDeserializer

      "RegisterPreAuthResponse" >> {
        "new one" >> {
          val source = """{"formUrl":"https://test.paymentgate.ru/testpayment/merchants/tooeezzy/payment_ru.html?mdOrder=9aa5adc0-2fc3-41c1-b19b-46c867a8300d","orderId":"9aa5adc0-2fc3-41c1-b19b-46c867a8300d"}"""
          deserializer.registerPreAuthResponse(source) === RegisterPreAuthResponse(
            formUrl = "https://test.paymentgate.ru/testpayment/merchants/tooeezzy/payment_ru.html?mdOrder=9aa5adc0-2fc3-41c1-b19b-46c867a8300d",
            transactionId = "9aa5adc0-2fc3-41c1-b19b-46c867a8300d"
          )
        }
        "duplicate" >> {
          val source = """{"errorCode":"1","errorMessage":"Order number is duplicated, order with given order number is processed already"}"""
          deserializer.registerPreAuthResponse(source) === RegisterPreAuthResponse(
            errorCode = "1",
            errorMessage = "Order number is duplicated, order with given order number is processed already"
          )
        }
      }

      "DepositResponse" >> {
        val source = """{"errorCode":"1","errorMessage":"Order number is duplicated, order with given order number is processed already"}"""
        deserializer.depositResponse(source) === DepositResponse(
          errorCode = "1",
          errorMessage = "Order number is duplicated, order with given order number is processed already"
        )
      }

      "GetOrderStatusExtendedResponse" >> {
        import GetOrderStatusExtendedResponse._

        "unknownOrderIdResponse" >> {
          val source = """{"ErrorCode":"6","ErrorMessage":"Unknown order id"}"""
          deserializer.getOrderStatusExtendedResponse(source) === GetOrderStatusExtendedResponse(
            errorCodeOpt = ErrorCode("6"),
            errorMessage = "Unknown order id"
          )
        }
        "rejectedOrderResponse" >> {
          val source = """{"expiration":"201512","cardholderName":"sdfgsdfg sdfgsdf","depositAmount":100,"currency":"810","approvalCode":"123456","authCode":2,"ErrorCode":"0","ErrorMessage":"Success","OrderStatus":2,"OrderNumber":"5675234","Pan":"411111**1111","Amount":100,"Ip":"93.174.72.206"}"""
          deserializer.getOrderStatusExtendedResponse(source) === GetOrderStatusExtendedResponse(
            errorCodeOpt = ErrorCode("0"),
            errorMessage = "Success"
          )
        }
        "okResponse" >> {
          val source = """{"expiration":"201312","cardholderName":"sfgsdf sdsd","depositAmount":0,"currency":"810","authCode":2,"ErrorCode":"2","ErrorMessage":"Payment is declined","OrderStatus":6,"OrderNumber":"2389234073489423","Pan":"411111**1111","Amount":10099,"Ip":"93.174.72.206"}"""
          deserializer.getOrderStatusExtendedResponse(source) === GetOrderStatusExtendedResponse(
            errorCodeOpt = ErrorCode("2"),
            errorMessage = "Payment is declined"
          )
        }
      }
    }
  }
}