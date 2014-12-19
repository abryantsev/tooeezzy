package com.tooe.core.domain

case class AdminRoleId(id: String)

object AdminRoleId {
  val Admin = AdminRoleId("admin")
  val Superagent = AdminRoleId("superagent")
  val Agent = AdminRoleId("agent")
  val Superdealer = AdminRoleId("superdealer")
  val Dealer = AdminRoleId("dealer")
  val Moderator = AdminRoleId("moderator")
  val Accountant = AdminRoleId("accountant")
  val Client = AdminRoleId("client")
  val Activator = AdminRoleId("activator")
  val Exporter = AdminRoleId("exporter")

  val AllRoles = Set(Admin, Superagent, Superdealer, Dealer, Agent, Moderator, Accountant, Client, Activator, Exporter)
}