package com.tooe.core.util

import java.text.{DecimalFormat, DecimalFormatSymbols}

object FormatHelper {

  def formatBigDecimal(value: BigDecimal): String = {
    val fs = new DecimalFormatSymbols()
    fs.setDecimalSeparator('.')
    val df = new DecimalFormat("#", fs)
    df.setMaximumFractionDigits(2)
    df.setMinimumFractionDigits(0)
    df.setGroupingUsed(false)
    df.format(value)
  }

  def formatBigInt(value: BigInt): String = value.toString
}
