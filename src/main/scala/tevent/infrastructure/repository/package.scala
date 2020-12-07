package tevent.infrastructure

import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import tevent.domain.RepositoryError
import tevent.infrastructure.repository.tables._
import zio._

package object repository {
  type Db = Has[Db.Service]
  type Tables = UsersT with OrganizationsT with EventsT with OrgParticipantsT with EventParticipantsT with OrgParticipationRequestsT

  object Tables {
    def live: URLayer[Db, Tables] = ZLayer.fromServiceMany { d =>
        implicit val profile: JdbcProfile = d.profile
        val users = new UsersTable
        val organizations = new OrganizationsTable
        val events = new EventsTable(organizations)
        val eventParticipants = new EventParticipantsTable(users, events)
        val orgParticipants = new OrgParticipantsTable(users, organizations)
        val orgParticipationRequests = new OrgParticipationRequestsTable(users, organizations)
        Has(users) ++ Has(organizations) ++ Has(events) ++ Has(eventParticipants) ++ Has(orgParticipants) ++ Has(orgParticipationRequests)
      }

    def create: URLayer[Db with Tables, Db with Tables] = ZLayer.fromFunctionManyM { r: Db with Tables =>
      val db = r.get[Db.Service]
      val profile = db.profile
      import profile.api._

      val users = r.get[UsersTable]
      val organizations = r.get[OrganizationsTable]
      val orgParticipants = r.get[OrgParticipantsTable]
      val orgParticipationRequests = r.get[OrgParticipationRequestsTable]
      val events = r.get[EventsTable]
      val eventParticipants = r.get[EventParticipantsTable]

      val tables = List(users.All, organizations.All, orgParticipants.All, orgParticipationRequests.All, events.All, eventParticipants.All)
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

  private[repository] implicit class ZIOOps[R](private val dbio: DBIO[R]) extends AnyVal {
    def toZIO: RIO[Db.Service, R] =
      for {
        db <- ZIO.access[Db.Service](_.db)
        r  <- ZIO.fromFuture(implicit ec => db.run(dbio))
      } yield r
  }

  private[repository] implicit class TaskOps[T](private val task: Task[T]) extends AnyVal {
    def refineRepositoryError: IO[RepositoryError, T] = task.refineOrDie {
      case e: Exception => RepositoryError(e)
    }
  }
}
