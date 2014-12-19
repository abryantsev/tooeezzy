package com.tooe.core.payment.platron

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.payment.plantron.PlatronMessageSigner

class PlatronMessageSignerSpec extends SpecificationWithJUnit {

  val request = Seq(
     'pg_salt -> "9imM909TH820jwk387",
     'pg_t_param -> "value3",
     'pg_a_param -> "value1",
     'pg_b_param -> "value2"
  )
  val expectedParams = Seq(
    'pg_a_param -> "value1",
    'pg_b_param -> "value2",
    'pg_salt -> "9imM909TH820jwk387",
    'pg_t_param -> "value3"
  )
  val expectedParamValues = expectedParams map (_._2) mkString ";"
  val secretKey = "SomeSecretKey"
  val script = "payment.php"
  val expectedContentForSignature = s"$script;$expectedParamValues;$secretKey"
  val expectedSignature = "81b5e1630aa173cb5a13f4f164d49296"

  "PlatronMessageSigner" should {
    import PlatronMessageSigner._
    "extract params from request in alphabetical order" >> {
      paramsToSign(request) must_== expectedParams
    }
    "extract parameter value list" >> {
      extractParamValues(request) must_== expectedParamValues
    }
    "make content string to calculate signature" >> {
      prepareContentForSignature(script, request, secretKey) must_== expectedContentForSignature
    }
    "calculate signature" >> {
      calcSignature(script, request, secretKey) must_== expectedSignature
    }
    "sign requests and extract signatures" >> {
      val signedRequest = sign(script, request, secretKey)
      extractSignature(signedRequest) must_== Some(expectedSignature)
    }
    "check signature" >> {
      val signedRequest = sign(script, request, secretKey)
      checkSignature(script, signedRequest, secretKey) must beTrue
    }
  }
}