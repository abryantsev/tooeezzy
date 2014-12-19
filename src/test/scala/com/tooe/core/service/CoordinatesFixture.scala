package com.tooe.core.service

import com.tooe.core.domain.Coordinates
import scala.util.Random

class CoordinatesFixture {
  val coords = Coordinates(Random.nextDouble() % 45 , Random.nextDouble() % 45)
}