package com.tooe.api.validation

import com.tooe.core.db.mongo.util.HasIdentity

trait Validatable {

  def check: Unit = ValidationHelper.checkObject(this)

  def validate(ctx: ValidationContext): Unit
}

class ValidationContext {
  var result: ValidationResult = ValidationSucceed
  
  def fail(message: String): Unit = apply(ValidationFailed(message))

  def checkOnlyOneAllowed[T <: HasIdentity](incompatible: Set[T], values: Set[T]) = {
    val incompatibleValues = incompatible intersect values
    if (incompatibleValues.size > 1) {
      val incompatibleValuesStr = incompatibleValues map (_.id) mkString ("[", ", ", "]")
      fail(s"Only one of $incompatibleValuesStr is allowed")
    }
  }

  def apply(validation: => ValidationResult): Unit = result &= validation
}