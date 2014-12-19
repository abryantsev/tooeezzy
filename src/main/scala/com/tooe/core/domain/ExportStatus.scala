package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait ExportStatus extends HasIdentity

object ExportStatus extends HasIdentityFactoryEx[ExportStatus]{

  case object Exported extends ExportStatus {
    def id = "exported"
  }

  val values = Seq(Exported)

}
