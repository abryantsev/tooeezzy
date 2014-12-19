package com.tooe.api.validation

object EmailValidator {
  val EmailRegex = """.+@.+\..+""".r

  def apply(email: String): ValidationResult = email match {
    case EmailRegex() => ValidationSucceed
    case _ => ValidationFailed(ErrorMessage("invalid email format: " + email, code = 0))
  }
}