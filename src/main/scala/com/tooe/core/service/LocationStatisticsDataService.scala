package com.tooe.core.service

import com.tooe.core.db.mongo.domain.LocationStatistics
import com.tooe.core.domain.LocationStatisticsId
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.LocationStatisticsRepository

trait LocationStatisticsDataService {

  def save(entity: LocationStatistics): LocationStatistics

  def findOne(id: LocationStatisticsId): Option[LocationStatistics]

}

@Service
class LocationStatisticsDataServiceImpl extends LocationStatisticsDataService {

  @Autowired var repo: LocationStatisticsRepository = _

  def save(entity: LocationStatistics) = repo.save(entity)

  def findOne(id: LocationStatisticsId) = Option(repo.findOne(id.id))
}
