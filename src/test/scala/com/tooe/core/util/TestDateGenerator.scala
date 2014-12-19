package com.tooe.core.util

import java.util.Date

class TestDateGenerator(start: Date = new Date, step: Long = 1000, multiplicity: Int = 1) {
  var value: Long = start.getTime

  def next(step: Long = step, multiplicity: Int = multiplicity): Date = {
    value = value + step
    new Date(value / multiplicity * multiplicity)
  }
}