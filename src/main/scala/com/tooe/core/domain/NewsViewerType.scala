package com.tooe.core.domain

sealed trait NewsViewerType

object NewsViewerType {

  case object ActorOrRecipient extends NewsViewerType
  case object Viewer extends NewsViewerType

}