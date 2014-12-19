package com.tooe.core.usecase.present

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}
import org.springframework.data.domain.Sort

sealed trait PresentAdminSearchSortType extends HasIdentity with UnmarshallerEntity {
  def sortField: Seq[String]
  def sort = new Sort(Sort.Direction.ASC, sortField: _*)
}

object PresentAdminSearchSortType extends HasIdentityFactoryEx[PresentAdminSearchSortType] {

  object ProductName extends PresentAdminSearchSortType {
    def id = "productname"
    def sortField = Seq("p.pn")
  }

  object ProductType extends PresentAdminSearchSortType {
    def id = "producttype"
    def sortField = Seq("p.pt")
  }

  object CreationTime extends PresentAdminSearchSortType {
    def id = "creationtime"
    def sortField = Seq("t")
  }

  object ReceivedTime extends PresentAdminSearchSortType {
    def id = "receivedtime"
    def sortField = Seq("rt")
  }

  object Code extends PresentAdminSearchSortType {
    def id = "code"
    def sortField = Seq("c")
  }

  object Status extends PresentAdminSearchSortType {
    def id = "status"
    def sortField = Seq("cs")
  }

  def values = Seq(ProductName, ProductType, CreationTime, ReceivedTime, Code, Status)

  val Default = ProductName

}