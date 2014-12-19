package com.tooe.api.validation

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers
import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

class ValidationContextTest extends SpecificationWithJUnit with MustMatchers {

  "ValidationContext. onlyOneOfFor" should {

    sealed trait TestSet extends HasIdentity
    object TestSet extends HasIdentityFactoryEx[TestSet] {
      case object A extends TestSet {
        def id = "a"
      }
      case object B extends TestSet {
        def id = "b"
      }
      case object C extends TestSet {
        def id = "C"
      }
      def values = Seq(A, B, C)
    }
    import TestSet._

    val incompatible = Set[TestSet](A, B)

    "success when there is no incompatible values" >> {
      val vc = new ValidationContext()
      vc.checkOnlyOneAllowed(incompatible, Set[TestSet](A, C))

      vc.result.succeed === true
    }

    "fail when there are some incompatible value" >> {
      val vc = new ValidationContext()
      vc.checkOnlyOneAllowed(incompatible, Set[TestSet](A, C, B))

      vc.result.failed === true
      vc.result.messages.head.message must contain ("[a, b]")
    }
  }
}