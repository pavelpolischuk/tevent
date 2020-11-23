package tevent.infrastructure

import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
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
      val events = r.get[EventsTable]
      val organizations = r.get[OrganizationsTable]
      val orgParticipants = r.get[OrgParticipantsTable]
      val orgParticipationRequests = r.get[OrgParticipationRequestsTable]
      val eventParticipants = r.get[EventParticipantsTable]

      val createSchema = (users.All.schema ++ events.All.schema ++ organizations.All.schema
        ++ orgParticipants.All.schema ++ orgParticipationRequests.All.schema ++ eventParticipants.All.schema).createIfNotExists
      createSchema.toZIO.provide(db).orDie.map(_ => r)
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
