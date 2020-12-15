package tevent.main

import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import tevent.core.Db
import tevent.core.Db.ZIOOps
import tevent.events.repository.tables.{EventParticipantsTable, EventsTable}
import tevent.events.repository.{EventParticipantsT, EventsT}
import tevent.organizations.repository.tables.{OrgParticipantsTable, OrgParticipationRequestsTable, OrganizationTagsTable, OrganizationsTable}
import tevent.organizations.repository.{OrgParticipantsT, OrgParticipationRequestsT, OrganizationTagsT, OrganizationsT}
import tevent.user.repository.{UsersT, UsersTable}
import zio.{Has, URLayer, ZIO, ZLayer}

object Tables {
  type AllT = UsersT with OrganizationsT with EventsT with OrgParticipantsT with EventParticipantsT with OrgParticipationRequestsT with OrganizationTagsT

  def live: URLayer[Db, AllT] = ZLayer.fromServiceMany { d =>
    implicit val profile: JdbcProfile = d.profile

    val users = new UsersTable
    val organizations = new OrganizationsTable
    val events = new EventsTable(organizations)
    val eventParticipants = new EventParticipantsTable(users, events)
    val orgParticipants = new OrgParticipantsTable(users, organizations)
    val orgParticipationRequests = new OrgParticipationRequestsTable(users, organizations)
    val organizationTags = new OrganizationTagsTable(organizations)
    Has(users) ++ Has(organizations) ++ Has(events) ++ Has(eventParticipants) ++ Has(orgParticipants) ++ Has(orgParticipationRequests) ++ Has(organizationTags)
  }

  def createTables: URLayer[Db with AllT, Db with AllT] = ZLayer.fromFunctionManyM { r: Db with AllT =>
    val db = r.get[Db.Service]
    val profile = db.profile
    import profile.api._

    val users = r.get[UsersTable]
    val organizations = r.get[OrganizationsTable]
    val orgParticipants = r.get[OrgParticipantsTable]
    val orgParticipationRequests = r.get[OrgParticipationRequestsTable]
    val events = r.get[EventsTable]
    val eventParticipants = r.get[EventParticipantsTable]
    val organizationTags = r.get[OrganizationTagsTable]

    val tables = List(users.All, organizations.All, orgParticipants.All,
      orgParticipationRequests.All, events.All, eventParticipants.All, organizationTags.All)
    val existing = MTable.getTables.toZIO

    existing.flatMap(v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter(table =>
        !names.contains(table.baseTableRow.tableName)).map(_.schema.create)
      ZIO.foreach(createIfNotExist)(_.toZIO)
    })
      .provide(db)
      .orDie.as(r)
  }
}
