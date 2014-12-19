package com.tooe.core.db.mysql

import annotation.meta.field
import javax.persistence

package object domain {
  type Column = persistence.Column @field
  type Id = persistence.Id @field
  type GeneratedValue = persistence.GeneratedValue @field
  type MapsId = persistence.MapsId @field
  type OneToOne = persistence.OneToOne @field
  type JoinColumn = persistence.JoinColumn @field
}