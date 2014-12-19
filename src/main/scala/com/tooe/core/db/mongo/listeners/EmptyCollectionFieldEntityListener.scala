package com.tooe.core.db.mongo.listeners

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import com.mongodb.DBObject
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.domain.Field
import java.util
import scala.reflect.ClassTag
import com.tooe.core.db.mongo.domain.ArrayList

/**
 * Prevents persisting Option fields that have None value
 * Replaces null Option field values with Nones when entity has been read from the db
 */
class EmptyCollectionFieldEntityListener extends AbstractMongoEventListener[AnyRef] {

  override def onBeforeSave(source: AnyRef, dbo: DBObject) {
    val keys = dbo.keySet().asScala
    val sourceClass = source.getClass
    for (
      field <- sourceClass.getDeclaredFields;
      fieldName = dbFieldName(field) if keys.contains(fieldName)
    ) {
      field.setAccessible(true)
      val value = field.get(source)
      val dropField = value match {
        case None => true
        case list: util.ArrayList[_] if list.isEmpty => true
        case Nil => true
        case _ => false
      }
      if (dropField) {
        dbo.removeField(fieldName)
      }
    }
    super.onBeforeSave(source, dbo)
  }

  override def onAfterConvert(dbo: DBObject, source: AnyRef) {
    val keys = dbo.keySet().asScala.toSet
    setEmpty[Option[_]](None)(keys, source)
    setEmpty[ArrayList[_]](ArrayList.empty)(keys, source)
    setEmpty[Seq[_]](Nil)(keys, source)
    super.onAfterConvert(dbo, source)
  }

  private def setEmpty[T : ClassTag](emptyVal: T)(keys: Set[String], source: AnyRef) {
    val collectionClass = reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]]
    val sourceClass = source.getClass
    for (
      optionField <- sourceClass.getDeclaredFields if optionField.getType == collectionClass;
      fieldName = dbFieldName(optionField) if !keys.contains(fieldName)
    ) {
      optionField.setAccessible(true)
      val value = optionField.get(source)
      if (value == null) {
        optionField.set(source, emptyVal)
      }
    }
  }

  private def dbFieldName(field: java.lang.reflect.Field) =
    Option(field.getAnnotation(classOf[Field])) map (_.value) getOrElse field.getName
}