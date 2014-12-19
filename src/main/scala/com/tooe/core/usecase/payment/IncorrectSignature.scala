package com.tooe.core.usecase.payment

case class IncorrectSignature(message: String) extends Exception