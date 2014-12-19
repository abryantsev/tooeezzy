package com.tooe.core.payment.platron

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.payment.plantron._

class PlatronResponseBuilderSpec extends SpecificationWithJUnit {

  import com.tooe.core.util.SomeWrapper._

  val checkResponse = PlatronResponse(
    status = PlatronResponseStatus.Ok,
    timeout = 100,
    description = "some description",
    errorDescription = "error description"
  )

  val expectedResponse: PlatronParams = {
    import checkResponse._
    Seq(
      'pg_status -> "ok",
      'pg_timeout -> "100",
      'pg_description -> description.get,
      'pg_error_description -> errorDescription.get,
      'pg_salt -> salt
    )
  }

  "PlatronResponseBuilder" should {
    import PlatronResponseBuilder._
    "build proper platron parms" >> {
      build(checkResponse).toMap must_== expectedResponse.toMap
    }
  }
}