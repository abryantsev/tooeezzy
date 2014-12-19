package com.tooe.core.payment.plantron

trait PlatronMessageBuilder {

  def optParam(name: Symbol, value: Option[String]): PlatronParams = value.map(name -> _).toSeq
}