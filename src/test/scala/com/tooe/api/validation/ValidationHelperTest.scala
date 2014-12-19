package com.tooe.api.validation

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers
import com.tooe.api.JsonProp
import java.util.Date
import com.tooe.core.util.DateHelper

class ValidationHelperTest extends SpecificationWithJUnit with MustMatchers {

  "ValidationHelper" should {
    import ValidationHelper._

    "succeess if there is none of fields that are null" >> {
      val entity = Entity(field1 = "not null")
      val result = checkNulls(entity)
      result.succeed must beTrue
    }
    "fail if there is null field" >> {
      val entity = Entity(field1 = null)
      val result = checkNulls(entity)
      result.failed must beTrue
      result.messages must not (beEmpty)
      result.messages.map(_.message).head contains ("field1")
    }
    "fail if there is null field in subentity" >> {
      val entity = ComplexEntity()
      val result = checkRecursively(entity, ValidationHelper.checkNulls)
      result.failed must beTrue
      result.messages.size === 2
    }
    "pass simple types" >> {
      val entity = WithSimpleFields()
      fields(entity).size === 1
      checkRecursively(entity, ValidationHelper.checkNulls).succeed must beTrue
    }
    "pass inner Seq" >> {
      val entity = WithInnerSeq(Seq("something"))
      checkRecursively(entity, ValidationHelper.checkNulls).succeed must beTrue
    }
    "validate inner Seq" >> {
      val entity = WithComplexInnerSeq(Seq(Entity()))
      val result = checkRecursively(entity, ValidationHelper.checkNulls)
      result.failed must beTrue
      result.messages.size === 1
    }
    "validate inner Option" >> {
      val entity = WithComplexInnerOpt(Some(Entity()))
      val result = checkRecursively(entity, ValidationHelper.checkNulls)
      result.failed must beTrue
      result.messages.size === 1
    }
    "use path to identify fields" >> {
      case class Entity(s: SubEntity)
      case class SubEntity(f: String = null)
      val entity = Entity(SubEntity())
      checkRecursively(entity, ValidationHelper.checkNulls).messages.map(_.message).head must startWith ("s.f")
    }
    "use JsonProp annotation field name" >> {
      case class Entity(s: SubEntity)
      case class SubEntity(@JsonProp("field") f: String = null)
      val entity = Entity(SubEntity())
      checkRecursively(entity, ValidationHelper.checkNulls).messages.map(_.message).head must startWith ("s.field")
    }
    "run validate" >> {
      case class SomeClass(inner: SomeInner)
      case class SomeInner(email: String) extends Validatable {
        def validate(ctx: ValidationContext) = {
          ctx(EmailValidator(email))
        }
      }
      val entity = SomeClass(SomeInner(email = "a.b@c.d"))
      validateRequest(entity) must not (throwA[ValidationException])

      val entity2 = SomeClass(SomeInner(email = "a.b@d"))
      validateRequest(entity2) must (throwA[ValidationException])
    }
    "skip non project objects" >> {
      val request = WithDate()
      validateRequest(request) must not (throwA[ValidationException])
    }
    "skip optional non project objects" >> {
      val request = WithDateOpt()
      validateRequest(request) must not (throwA[ValidationException])
    }
    "skip seq of non project objects" >> {
      val request = WithDateSeq()
      validateRequest(request) must not (throwA[ValidationException])
    }
  }
}

case class Entity
(
  field1: String = null,
  field2: Option[String] = None
  )

case class ComplexEntity
(
  field1: String = null,
  field2: Entity = Entity()
  )

case class WithSimpleFields(field1: Int = 0)

case class WithInnerSeq(field1: Seq[String])

case class WithComplexInnerSeq(field1: Seq[Entity])

case class WithComplexInnerOpt(field1: Option[Entity])

case class ValidateEntity(email: String = null) {
  final val ExpectedResult = ValidationFailed(Seq(ErrorMessage("Expected Failure", 0)))
  def validate(path: String): ValidationResult = ExpectedResult
}

case class WithDate(field1: Date = DateHelper.currentDate)

case class WithDateOpt(field1: Option[Date] = Some(DateHelper.currentDate))

case class WithDateSeq(field1: Seq[Date] = Seq(DateHelper.currentDate))