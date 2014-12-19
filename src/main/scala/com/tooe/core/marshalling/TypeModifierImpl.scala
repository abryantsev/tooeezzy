package com.tooe.core.marshalling

import com.fasterxml.jackson.databind.`type`._
import com.fasterxml.jackson.databind.JavaType
import java.lang.reflect.{ParameterizedType, Type}

trait TypeModifierImpl extends TypeModifier {

  def BASE: Class[_]

  def UNKNOWN = SimpleType.construct(classOf[AnyRef])

  override def modifyType(originalType: JavaType, jdkType: Type, context: TypeBindings, typeFactory: TypeFactory) =
    if (originalType.containedTypeCount() > 1) originalType
    else
      classObjectFor(jdkType) find (BASE.isAssignableFrom(_)) map {
        cls =>
          val eltType = if (originalType.containedTypeCount() == 1) originalType.containedType(0) else UNKNOWN
          typeFactory.constructCollectionLikeType(cls, eltType)
      } getOrElse originalType

  protected def classObjectFor(jdkType: Type) = jdkType match {
    case cls: Class[_] => Some(cls)
    case pt: ParameterizedType => pt.getRawType match {
      case cls: Class[_] => Some(cls)
      case _ => None
    }
    case _ => None
  }
}