package com.tooe.core.usecase.payment

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers
import com.tooe.core.util.HashHelper
import org.bson.types.ObjectId
import com.tooe.api.validation.ValidationHelper
import com.tooe.api.validation.ValidationException
import com.tooe.core.domain.{UserId, CountryId}

class PaymentRequestValidationSpec extends SpecificationWithJUnit with MustMatchers {

  val paymentSystemsRequirePaymentUserId = Seq("ALFACLICK", "MONEYMAILRU")

  "payment.PaymentSystem" should {
    "fail when paymentUserId wasn't specified for certain Payment Systems" >> {
      for (ps <- paymentSystemsRequirePaymentUserId) {
        PaymentSystem(system = ps) must throwA[IllegalArgumentException] //TODO need more specified exception to distinguish it from the all the others
      }
    }
    "pass validation for all the other systems" >> {
      PaymentSystem(system = HashHelper.uuid) must not (throwA[IllegalAccessException])
    }
  }
  
  "PaymentRequest" should {
    import ValidationHelper._
    val f = new PaymentRequestRecipientFixture
    "fail if more then one recipient contacts specified" >> {
      validateRequest(f.full) must throwA[ValidationException]
    }
    "fail if none of recipient contacts specified" >> {
      validateRequest(f.empty) must throwA[ValidationException]
    }
    "pass id only" >> {
      validateRequest(f.idOnly) must not (throwA[ValidationException])
    }
    "pass email only" >> {
      validateRequest(f.emailOnly) must not (throwA[ValidationException])
    }
    "pass phone with country only" >> {
      validateRequest(f.phoneWithCountry) must not (throwA[ValidationException])
    }
  }
}

class PaymentRequestRecipientFixture {
  
  import com.tooe.core.util.SomeWrapper._
  
  val userId = UserId(new ObjectId)
  val countryId = CountryId(HashHelper.uuid.take(2))
  
  val full = Recipient(
    id = userId,
    email = "some-email-id",
    phone = "some-phone",
    country = CountryParams(countryId, "code")
  )

  val empty = Recipient()
  
  val idOnly = Recipient(id = userId)
  val emailOnly = Recipient(email = "some-email-id")
  val phoneWithCountry = Recipient(phone = "some-phone", country = CountryParams(countryId, "code"))
}