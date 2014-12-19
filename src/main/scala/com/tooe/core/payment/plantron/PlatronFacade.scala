package com.tooe.core.payment.plantron

import com.tooe.api.service.Utf8EncodingContentTypeHelper
import spray.http.FormData
import spray.httpx.marshalling._

object PlatronFacade extends Utf8EncodingContentTypeHelper {

  private val PaymentScript = "payment.php"
  private val PaymentUrl = s"https://www.platron.ru/$PaymentScript"

  def paymentGetUrl(request: PlatronRequest, secretKey: String): String = {
    val requestParams = PlatronRequestBuilder.build(request)
    val signedParams = PlatronMessageSigner.sign(PaymentScript, requestParams, secretKey)
    val signedStringParams = signedParams map {
      case (k, v) => k.name -> v
    }
    val formData = FormData(signedStringParams.toMap)
    val asString = marshal(formData)
    val entity = asString.right.get

    PaymentUrl + "?" + entity.asString
  }
  
  def parseXml(body: String): PlatronParams = {
    val xmlRequest = XmlMessageHelper.extractXmlRequest(body)
    XmlMessageHelper.parse(xmlRequest)
  }
}