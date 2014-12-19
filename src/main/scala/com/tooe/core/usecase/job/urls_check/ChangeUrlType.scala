package com.tooe.core.usecase.job.urls_check

import com.tooe.core.db.mongo.domain.Urls
import com.tooe.core.domain.MediaObjectId

object ChangeUrlType {

  case class ChangeTypeToS3(url: Urls, newMediaId: MediaObjectId)
  case class ChangeTypeToCDN(url: Urls)
}
