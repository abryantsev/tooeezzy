package com.tooe.core.payment.platron

import org.specs2.mutable.SpecificationWithJUnit
import xml.XML
import com.tooe.core.payment.plantron.XmlMessageHelper

class XmlMessageHelperSpec extends SpecificationWithJUnit {

  val xml =
    <request>
      <key1>value1</key1>
      <key2>value2</key2>
      <sub>
         <subKey3>ignorable inner element</subKey3>
      </sub>
      <z/>
    </request>

  val params = Seq(
    'key1 -> "value1",
    'key2 -> "value2",
    'z -> ""
  )

  val requestBody = "pg_xml=%3C%3Fxml+version%3D%221.0%22+encoding%3D%22utf-8%22%3F%3E%0A%3Crequest%3E%3Cpg_salt%3Ea8c008a%3C%2Fpg_salt%3E%3Cpg_order_id%3E%3C%2Fpg_order_id%3E%3Cpg_payment_id%3E5659046%3C%2Fpg_payment_id%3E%3Cpg_amount%3E9.9900%3C%2Fpg_amount%3E%3Cpg_currency%3ERUR%3C%2Fpg_currency%3E%3Cpg_ps_amount%3E9.99%3C%2Fpg_ps_amount%3E%3Cpg_ps_full_amount%3E9.99%3C%2Fpg_ps_full_amount%3E%3Cpg_ps_currency%3ERUR%3C%2Fpg_ps_currency%3E%3Cpg_payment_system%3ETEST%3C%2Fpg_payment_system%3E%3CrecipientId%3E5135e90d238c83cc8eac3772%3C%2FrecipientId%3E%3CisPrivate%3Etrue%3C%2FisPrivate%3E%3Cmsg%3EMessageContent%3C%2Fmsg%3E%3CproductId%3E5135e90d238c83cc8eac3771%3C%2FproductId%3E%3Cpg_sig%3Ecbbd508733388f15d4dd16a19a2dad2b%3C%2Fpg_sig%3E%3C%2Frequest%3E"

  "XmlMessageHelper" should {
    import XmlMessageHelper._
    "parse xml to seq as pairs of param and value ignoring any sub elements" >> {
      parse(xml) === params
    }
    "build xml from seq of pairs" >> {
      val builtXml = XML.loadString(build('request, params))
      parse(builtXml) === params
    }
    "parse xml request body" >> {
      val requestXml = extractXmlRequest(requestBody)
      requestXml must not (beEmpty)
    }
  }
}