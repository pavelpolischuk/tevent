package tevent.events.repository

import slick.dbio.DBIO
import tevent.core.Db.{TaskOps, ZIOOps}
import tevent.core.{Db, RepositoryError}
import tevent.events.model.{Event, EventFilter, EventParticipationType}
import tevent.events.repository.tables.{EventParticipantsTable, EventsTable}
import zio.{IO, Task, URLayer, ZLayer}

object SlickEventsRepository {
  def apply(db: Db.Service, events: EventsTable, participants: EventParticipantsTable): EventsRepository.Service =
    new EventsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def add(event: Event): IO[RepositoryError, Long] =
      io(events.add(event)).refineRepositoryError

    override def search(eventFilter: EventFilter): IO[RepositoryError, List[Event]] =
      io(events.where(eventFilter)).map(_.toList).refineRepositoryError

    override def getByUser(userId: Long): IO[RepositoryError, List[(Event, EventParticipationType)]] =
      io(participants.forUser(userId)).map(_.toList).refineRepositoryError

    override def getById(id: Long): IO[RepositoryError, Option[Event]] =
      io(events.withId(id)).refineRepositoryError

    override def update(event: Event): IO[RepositoryError, Unit] =
      io(events.update(event)).unit.refineRepositoryError
  }

  def live: URLayer[Db with EventsT with EventParticipantsT, EventsRepository] =
    ZLayer.fromServices[Db.Service, EventsTable, EventParticipantsTable, EventsRepository.Service](SlickEventsRepository.apply)
}
