package tevent.infrastructure

import slick.dbio.DBIO
import tevent.domain.RepositoryError
import tevent.infrastructure.repository.tables._
import zio._

package object repository {
  type Db = Has[Db.Service]
  type Tables = UsersT with OrganizationsT with EventsT with OrgParticipantsT with EventParticipantsT

  object Tables {
    def live: URLayer[Db, Tables] = ZLayer.fromServiceMany { d =>
        val profile = d.profile
        val users = new UsersTable(profile)
        val organizations = new OrganizationsTable(profile)
        val events = new EventsTable(profile, organizations)
        val eventParticipants = new EventParticipantsTable(profile, users, events)
        val orgParticipants = new OrgParticipantsTable(profile, users, organizations)
        Has(users) ++ Has(organizations) ++ Has(events) ++ Has(eventParticipants) ++ Has(orgParticipants)
      }

    def create: URLayer[Db with Tables, Db with Tables] = ZLayer.fromFunctionManyM { r: Db with Tables =>
      val db = r.get[Db.Service]
      val profile = db.profile
      import profile.api._

      val users = r.get[UsersTable]
      val events = r.get[EventsTable]
      val organizations = r.get[OrganizationsTable]
      val orgParticipants = r.get[OrgParticipantsTable]
      val eventParticipants = r.get[EventParticipantsTable]

      val createSchema = (users.All.schema ++ events.All.schema ++ organizations.All.schema
        ++ orgParticipants.All.schema ++ eventParticipants.All.schema).createIfNotExists
      ZIO.fromDBIO(createSchema).provide(db).orDie.map(_ => r)
    }
  }

  private[repository] implicit class ZIOOps(private val obj: ZIO.type) extends AnyVal {
    def fromDBIO[R](dbio: DBIO[R]): RIO[Db.Service, R] =
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
