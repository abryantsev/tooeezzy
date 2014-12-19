package com.tooe.api.validation

import java.lang.reflect.Field
import spray.http.{StatusCodes, StatusCode}

object ValidationHelper extends ValidationHelper

trait ValidationHelper {

  final val DomainClassPrefix = "com.tooe"
  
  type Path = Seq[String]
  
  val EmptyPath: Path = Seq()
  
  def validateRequest(obj: AnyRef, statusCode: StatusCode = StatusCodes.BadRequest) {
    ValidationHelper.checkRecursively(obj, ValidationHelper.checkNulls).throwIfFailed(statusCode)
    ValidationHelper.checkRecursively(obj, ValidationHelper.validate).throwIfFailed(statusCode)
  }

  def checkRecursively(obj: AnyRef, checkFun: (AnyRef, Path) => ValidationResult, path: Path = EmptyPath): ValidationResult = {
    val result = checkFun(obj, path)

    val inners = getCollectionMembers(obj) getOrElse getComplexMembers(obj)

    val innerResult = inners map {
      case (value, subpath) => checkRecursively(value, checkFun, path :+ subpath)
    } reduceOption (_ & _) getOrElse ValidationSucceed

    result & innerResult
  }

  private def getCollectionMembers(obj: AnyRef): Option[Seq[(AnyRef, String)]] = {
    val collectionObjs = obj match {
      case s: Seq[_] if s.size > 0 => Some(s.asInstanceOf[Seq[AnyRef]])
      case _ => None
    }
    val collectionMembers = collectionObjs map {
      _.zipWithIndex map { case (obj, index) => (obj, "["+index+"]") }
    }
    collectionMembers
  }

  private def getComplexMembers(obj: AnyRef): Seq[(AnyRef, String)] = {
    val fs = fields(obj)

    def isProjectObject(v: AnyRef) =
      v != obj && v != null && Option(v.getClass.getPackage).map(_.getName.startsWith(DomainClassPrefix)).getOrElse(false)

    val complexMembers = fs collect {
      case (f, Some(v: AnyRef)) if isProjectObject(v) => (v, f)
      case (f, v: Seq[_]) if v.size > 0 => (v.asInstanceOf[Seq[AnyRef]] filter isProjectObject, f)
      case (f, v: AnyRef) if isProjectObject(v) => (v, f)
    }
    complexMembers
  }

  def checkNulls(obj: AnyRef, path: Path = EmptyPath): ValidationResult = {
    val nullFields = fields(obj) collect {
      case (f, v) if v == null => f
    }
    if (nullFields.size == 0) ValidationSucceed
    else ValidationFailed(notSpecifiedFields(nullFields map (path :+ _)))
  }

  def notSpecifiedFields(paths: Seq[Path]) = paths map renderPath map ErrorMessage.notSpecifiedField

  def renderPath(path: Path): String = path mkString "."

  def validate(obj: AnyRef, path: Path = EmptyPath): ValidationResult = obj match {
    case o: Validatable => validate(o)
    case _ => ValidationSucceed
  }
  
  def validate(obj: Validatable): ValidationResult = {
    val ctx = new ValidationContext
    obj.validate(ctx)
    ctx.result
  }

  def fields(obj: AnyRef): Seq[(String, AnyRef)] =
    obj.getClass.getDeclaredFields map { field =>
      field.setAccessible(true)
      val value = field.get(obj)
      (fieldName(field), value)
    }

  def fieldName(field: Field): String =
    Option(field.getAnnotation(classOf[com.fasterxml.jackson.annotation.JsonProperty])) map (_.value()) getOrElse field.getName

  def check(requirementsChain: ValidationResult => ValidationResult, statusCode: StatusCode = StatusCodes.BadRequest): Unit =
    requirementsChain(ValidationSucceed).throwIfFailed(statusCode)

  def checkObject(obj: Validatable, statusCode: StatusCode = StatusCodes.BadRequest): Unit =
    validate(obj).throwIfFailed(statusCode)
}

case class ValidationException(result: ValidationFailed, statusCode: StatusCode) extends Exception("Validation exception")