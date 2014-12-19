package com.tooe.core.marshalling

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.`type`._
import deser.{ContextualDeserializer, Deserializers}
import com.fasterxml.jackson.module.scala.JacksonModule
import com.tooe.core.domain.Unsetable

object UnsetableModule extends UnsetableDeserializerModule

trait UnsetableDeserializerModule extends UnsetableTypeModifierModule {
  this += UnsetableDeserializerResolver
}

trait UnsetableTypeModifierModule extends JacksonModule {
  this += UnsetableTypeModifier
}

private object UnsetableTypeModifier extends TypeModifierImpl {
  def BASE = classOf[Unsetable[Any]]
}

private object UnsetableDeserializerResolver extends Deserializers.Base {
  val CLASS = classOf[Unsetable[AnyRef]]

  override def findCollectionLikeDeserializer(theType: CollectionLikeType,
                                              config: DeserializationConfig,
                                              beanDesc: BeanDescription,
                                              elementTypeDeserializer: TypeDeserializer,
                                              elementDeserializer: JsonDeserializer[_]) =
    if (!CLASS.isAssignableFrom(theType.getRawClass)) null
    else new UnsetableDeserializer(theType.containedType(0), elementDeserializer)
}

private class UnsetableDeserializer(elementType: JavaType, var deser: JsonDeserializer[_])
  extends JsonDeserializer[Unsetable[AnyRef]] with ContextualDeserializer {

  override def createContextual(ctxt: DeserializationContext, property: BeanProperty) =
    if (deser != null) this
    else new UnsetableDeserializer(elementType, ctxt.findContextualValueDeserializer(elementType, property))

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext) =
    if (jp.getText == "") Unsetable.Unset
    else Unsetable.Update(deser.deserialize(jp, ctxt).asInstanceOf[AnyRef])

  override def getNullValue = Unsetable.Skip
}