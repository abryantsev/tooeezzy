package com.tooe.core.db.graph.test.data

import com.tooe.core.domain.LocationId
import org.bson.types.ObjectId

trait TestDataFavorites {

  val locationId1, locationId2, locationId3  = new ObjectId().toString
//  val locationId1 = "41197909444fddfecf2a6688"
//  val locationId2 = "41197909444fddfecf2a6689"
//  val locationId3 = "41197909444fddfecf2a6687"

 val favorites12 = java.util.Arrays.asList(locationId1, locationId2)
 val favorites1 = java.util.Arrays.asList(locationId1)
 val emptyFavorites = new java.util.ArrayList[LocationId]()
 val wrongFavorites = java.util.Arrays.asList(testLocationId)

 val testLocationId = LocationId(new ObjectId())
 
}
