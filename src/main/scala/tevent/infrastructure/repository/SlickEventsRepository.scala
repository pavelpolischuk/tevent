package tevent.infrastructure.repository

import slick.dbio.DBIO
import tevent.domain.RepositoryError
import tevent.domain.model.Event
import tevent.domain.repository.EventsRepository
import tevent.infrastructure.repository.tables.{EventParticipantsT, EventParticipantsTable, EventsT, EventsTable}
import zio._

object SlickEventsRepository {
  def apply(db: Db.Service, table: EventsTable, participants: EventParticipantsTable): EventsRepository.Service = new EventsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = ZIO.fromDBIO(action).provide(db)

    override def add(event: Event): IO[RepositoryError, Long] =
      io(table.add(event)).refineRepositoryError

    override val getAll: IO[RepositoryError, List[Event]] =
      io(table.all).map(_.toList).refineRepositoryError

    def getByUser(userId: Long): IO[RepositoryError, List[Event]] = ???

    def getByOrganization(organizationId: Long): IO[RepositoryError, List[Event]] = ???

    override def getById(id: Long): IO[RepositoryError, Option[Event]] =
      io(table.withId(id)).refineRepositoryError

    override def update(event: Event): IO[RepositoryError, Unit] =
      io(table.update(event)).unit.refineRepositoryError
  }

  def live: URLayer[Db with EventsT with EventParticipantsT, EventsRepository] =
    ZLayer.fromServices[Db.Service, EventsTable, EventParticipantsTable, EventsRepository.Service]((d, t, p) => SlickEventsRepository(d, t, p))
}
