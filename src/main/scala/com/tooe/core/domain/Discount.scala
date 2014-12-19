package com.tooe.core.domain

import java.util.Date
import com.fasterxml.jackson.annotation.JsonProperty

case class Discount (
  @JsonProperty("percentage") percent: Percent,
  @JsonProperty("startdate") startDate: Option[Date] = None,
  @JsonProperty("enddate") endDate: Option[Date] = None
)
{
  def percentAt(date: Date): Option[Int] = {
    val afterStart = startDate map (_.before(date)) getOrElse true
    val beforeEnd = endDate map (_.after(date)) getOrElse true
    if (afterStart && beforeEnd) Some(percent.value) else None
  }
}