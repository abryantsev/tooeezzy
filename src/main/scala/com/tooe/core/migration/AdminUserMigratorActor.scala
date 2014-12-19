package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import com.tooe.core.usecase.AppActor
import com.tooe.core.usecase.admin_user.{AdminCredentialsDataActor, AdminUserDataActor}
import com.tooe.core.domain.{AdminRoleId, AdminUserId}
import com.tooe.core.db.mongo.domain.AdminCredentials
import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.migration.IdMappingDataActor.SaveIdMapping
import com.tooe.core.migration.db.domain.{MappingCollection, IdMapping}
import org.bson.types.ObjectId
import com.tooe.core.migration.api.{DefaultMigrationResult, MigrationResponse}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import java.util.Date

object AdminUserMigratorActor {
  final val Id = 'adminUserMigrator

  case class LegacyAdminUser(legacyid: Int, name: String, lastname: String, email: String, pwd: String, role: Int, registrationdate: Date) extends UnmarshallerEntity
}

class AdminUserMigratorActor extends MigrationActor {
  import AdminUserMigratorActor._

  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)
  lazy val adminCredentialsDataActor = lookup(AdminCredentialsDataActor.Id)
  lazy val dictionaryIdMappingActor = lookup(DictionaryIdMappingActor.Id)

  def receive = {
    case legacyAdmin: LegacyAdminUser =>
      val result = for {
        (admin, creds) <- convertLegacyAdminUser(legacyAdmin)
        _ <- saveAdminUser(admin)
        _ <- saveAdminCredentials(creds)
      } yield MigrationResponse(DefaultMigrationResult(legacyAdmin.legacyid, admin.id.id, "adm_user_migrator"))
      result.pipeTo(sender)
  }

  def convertLegacyAdminUser(legacyAdmin: LegacyAdminUser) = {
    for {
      nid <- exchangeLegacyAdminId(legacyAdmin.legacyid)
      role <- exchangeLegacyRoleId(legacyAdmin.role)
    } yield {
      val user = AdminUser(id = nid, name = legacyAdmin.name, lastName = legacyAdmin.lastname, role = role, registrationDate = legacyAdmin.registrationdate)
      val creds = AdminCredentials(adminUserId = user.id, userName = legacyAdmin.email, password = "", legacyPassword = Option(legacyAdmin.pwd))
      (user, creds)
    }
  }

  def saveAdminUser(user: AdminUser) =
    adminUserDataActor.ask(AdminUserDataActor.SaveAdminUser(user)).mapTo[AdminUser]

  def saveAdminCredentials(creds: AdminCredentials) =
    adminCredentialsDataActor.ask(AdminCredentialsDataActor.SaveAdminCredentials(creds)).mapTo[AdminCredentials]

  def exchangeLegacyAdminId(lid: Int) =
    idMappingDataActor.ask(SaveIdMapping(IdMapping(collection = MappingCollection.admUser, legacyId = lid, newId = new ObjectId()))).mapTo[IdMapping].map(idm => AdminUserId(idm.newId))

  def exchangeLegacyRoleId(lid: Int) =
    dictionaryIdMappingActor.ask(DictionaryIdMappingActor.GetRole(lid)).mapTo[AdminRoleId]
}