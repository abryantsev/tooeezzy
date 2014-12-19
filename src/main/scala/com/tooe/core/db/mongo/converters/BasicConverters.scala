package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.{SessionToken, Percent}

trait BasicConverters {

  implicit val bigIntConverter = new DBSimpleConverter[BigInt] {
    def serialize(value: BigInt) = value.toString
    def deserialize(source: Any) = BigInt(source.asInstanceOf[String])
  }

  implicit val intConverter = new DBSimpleConverter[Int] {
    def serialize(value: Int) = value
    def deserialize(source: Any) = source.asInstanceOf[Int]
  }

  implicit val longConverter = new DBSimpleConverter[Long] {
    def serialize(value: Long) = value
    def deserialize(source: Any) = source match {
      case i: Int => i.toLong //obviously this should work
      case l => l.asInstanceOf[Long]
    }
  }

  implicit val bigDecimalConverter = new DBSimpleConverter[BigDecimal] {
    def serialize(value: BigDecimal) = value.toDouble
    def deserialize(source: Any) = BigDecimal(source.toString)
  }

  implicit val percentConverter =  new DBSimpleConverter[Percent] {
    def serialize(value: Percent) = value.value
    def deserialize(source: Any) = Percent(source.asInstanceOf[Int])
  }

  implicit val doubleConverter =  new DBSimpleConverter[Double] {
    def serialize(value: Double) = value
    def deserialize(source: Any) = source.asInstanceOf[Double]
  }

  implicit val sessionTokenConverter =  new DBSimpleConverter[SessionToken] {
    def serialize(value: SessionToken) = value.hash
    def deserialize(source: Any) = SessionToken(source.toString)
  }

}