package com.tooe.core.db.mongo

import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.{Query, Update}
import com.mongodb.{BasicDBObject, WriteResult}
import scala.collection.JavaConverters._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.Unsetable
import com.tooe.core.db.mongo.converters.DBSimpleConverter
import com.tooe.core.util.Lang
import com.tooe.core.domain.Unsetable.Skip
import com.tooe.core.db.mongo.domain.ObjectMap

package object query {
  
  implicit class SortHelper(val query: Query) extends AnyVal {
    def asc(fields: String*) = sort(Sort.Direction.ASC, fields: _*)
    def desc(fields: String*) = sort(Sort.Direction.DESC, fields: _*)
    def sort(direction: Sort.Direction, fields: String*): Query = sort(new Sort(direction, fields.asJava))
    def sort(sort: Sort): Query = query.`with`(sort)
  }

  implicit class PagingHelper(val query: Query) extends AnyVal {
    def withPaging(offsetLimit: OffsetLimit) =
      query.limit(offsetLimit.limit).skip(offsetLimit.offset)
  }

  implicit class RichUpdate(val update: Update) extends AnyVal {
    def nonEmpty = update.getUpdateObject.keySet().size() > 0
  }

  implicit class UpdateHelper(val statement: Update) extends AnyVal {

    @deprecated("use setSkipUnset with Unsetable or setOrSkip instead")
    @deprecated("doesn't support our entity converters, can work only with entities that spring is able to convert to database objects")
    def setSkipUnset(field: String, value: Option[Any]): Update =
      if (value == null)
        statement.unset(field)
      else value map { v =>
        statement.set(field, v)
      } getOrElse statement

    def setSkipUnset[T : DBSimpleConverter](field: String, value: Unsetable[T]): Update =
      extendUnset(field, value)((v, update) => update.set(field, serialize(v)))

    def setSkipUnset[T : DBSimpleConverter](field: String, valueOpt: Option[Unsetable[T]]): Update =
      valueOpt.map(value => setSkipUnset(field, value)).getOrElse(setSkipUnset(field,Unsetable.Skip))

    def setSkipUnsetSeq[T : DBSimpleConverter](field: String, value: Unsetable[Seq[T]]): Update =
      extendUnset(field, value)((v, update) => update.set(field, v map (serialize(_))))

    def setSkipUnsetSeq[T : DBSimpleConverter](field: String, valueOpt: Option[Unsetable[Seq[T]]]): Update =
      valueOpt.map(value => setSkipUnsetSeq(field, value)).getOrElse(setSkipUnsetSeq(field,Unsetable.Skip))

    def extendUnset[T](field: String, value: Unsetable[T])(f: (T, Update) => Update): Update = value match {
      case Unsetable.Update(v) => f(v, statement)
      case Unsetable.Unset     => statement.unset(field)
      case Unsetable.Skip      => statement
    }

    def setOrSkipSeq[T : DBSimpleConverter](field: String, value: Option[Seq[T]]): Update =
      value map { seq =>
        val serializedSeq = seq.map { v => serialize(v) }
        statement.set(field, serializedSeq)
      } getOrElse statement

    def setOrSkip[T : DBSimpleConverter](field: String, value: Option[T]): Update =
      value map { v => statement.set(field, serialize(v)) } getOrElse statement

    def setSerialize[T : DBSimpleConverter](field: String, value: T): Update = statement.set(field, serialize(value))

    def setSerializeSeq[T : DBSimpleConverter](field: String, value: Seq[T]): Update = statement.set(field, value map (v => serialize(v)))

    def setSerialize[T : DBSimpleConverter](field: String, source: ObjectMap[T]): Update = {
      val converter = implicitly[DBSimpleConverter[T]]
      val serializedMap = new BasicDBObject
      for ((k, v) <- source) {
        serializedMap.put(k, converter deserialize v)
      }
      statement.set(field, serializedMap)
    }


    private def serialize[T : DBSimpleConverter](value: T): Any = implicitly[DBSimpleConverter[T]] serialize value
  }

  implicit class WriteResultHelper(val writeResult: WriteResult) extends AnyVal {

    import UpdateResult._

    def asUpdateResult: UpdateResult =
      if (writeResult.getLastError.get("updatedExisting") == true) Updated
      else if (writeResult.getLastError.get("code") == "11001") NoUpdate  // code 11001: duplicate key error index
      else NotFound

    def asUpsertResult: UpsertResult =
      if (writeResult.getLastError.get("updatedExisting") == true) UpsertResult.Updated
      else UpsertResult.Inserted
  }

  implicit def LocalizedFieldToString(field: LocalizedField): String = field.value

}