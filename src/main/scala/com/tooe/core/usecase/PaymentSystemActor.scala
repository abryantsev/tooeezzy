package com.tooe.core.usecase

import com.tooe.core.application.Actors
import spray.http._
import akka.util.Timeout
import com.tooe.core.payment.plantron._
import akka.io.IO
import spray.can.Http
import spray.httpx.RequestBuilding._
import HttpCharsets.`UTF-8`

object PaymentSystemActor {
  final val Id = Actors.PlatronPaymentSystem

  case class GetPaymentSystems(amount: payment.Amount)
}

class PaymentSystemActor extends AppActor {
  import PaymentSystemActor._

  import context.system
  import context.dispatcher

  override implicit val timeout = Timeout(settings.Platron.PaymentSystem.Timeout)

  def testingMode = settings.Platron.TestingMode
  def platronPaymentSystemHost = settings.Platron.PaymentSystem.Host

  def script = settings.Platron.PaymentSystem.Script

  def receive = {
    case GetPaymentSystems(payment.Amount(value, currency)) =>
      val request = PlatronPaymentSystemRequest(
        merchantId = settings.Platron.MerchantId,
        amount = value,
        currency = Some(currency),
        testingMode = testingMode
      )

      val params = PlatronPaymentSystemRequestBuilder.build(request)
      val secretKey = settings.Platron.SecretKey
      val signedRequest = PlatronMessageSigner.sign(script, params, secretKey)
      val xmlRequest = XmlMessageHelper.build('request, signedRequest)
      val entity = HttpEntity(xmlRequest)
      val uri = Uri(platronPaymentSystemHost+script)

      (IO(Http) ? Post(uri, entity).withHostHeader).mapTo[HttpResponse] map { httpResponse =>
        log.info("Post: {}\n{}\nResponse: {}", uri, entity, httpResponse)
        val rawResponse = httpResponse.entity asString `UTF-8`
        PaymentSystemResponseParser.parse(rawResponse)
      } pipeTo sender
  }
}