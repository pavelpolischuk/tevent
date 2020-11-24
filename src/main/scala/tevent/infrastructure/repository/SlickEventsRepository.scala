package tevent.infrastructure.repository

import slick.dbio.DBIO
import tevent.domain.RepositoryError
import tevent.domain.model.{Event, EventParticipation, EventParticipationType, User}
import tevent.domain.repository.EventsRepository
import tevent.infrastructure.repository.tables.{EventParticipantsT, EventParticipantsTable, EventsT, EventsTable}
import zio._

object SlickEventsRepository {
  def apply(db: Db.Service, table: EventsTable, participants: EventParticipantsTable): EventsRepository.Service = new EventsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def add(event: Event): IO[RepositoryError, Long] =
      io(table.add(event)).refineRepositoryError

    override val getAll: IO[RepositoryError, List[Event]] =
      io(table.all).map(_.toList).refineRepositoryError

    override def getByOrganization(organizationId: Long): IO[RepositoryError, List[Event]] =
      io(table.ofOrganization(organizationId)).map(_.toList).refineRepositoryError

    override def getByUser(userId: Long): IO[RepositoryError, List[(Event, EventParticipationType)]] =
      io(participants.forUser(userId)).map(_.toList).refineRepositoryError

    override def getById(id: Long): IO[RepositoryError, Option[Event]] =
      io(table.withId(id)).refineRepositoryError

    override def update(event: Event): IO[RepositoryError, Unit] =
      io(table.update(event)).unit.refineRepositoryError

    override def getUsers(eventId: Long): IO[RepositoryError, List[(User, EventParticipationType)]] =
      io(participants.getUsersBy(eventId)).map(_.toList).refineRepositoryError

    override def checkUser(userId: Long, eventId: Long): IO[RepositoryError, Option[EventParticipationType]] =
      io(participants.checkUserIn(userId, eventId)).refineRepositoryError

    override def addUser(participation: EventParticipation): IO[RepositoryError, Unit] =
      io(participants.addUserTo(participation)).unit.refineRepositoryError

    override def updateUser(participation: EventParticipation): IO[RepositoryError, Unit] =
      io(participants.update(participation)).unit.refineRepositoryError

    override def removeUser(userId: Long, eventId: Long): IO[RepositoryError, Unit] =
      io(participants.removeUserFrom(userId, eventId)).unit.refineRepositoryError
  }

  def live: URLayer[Db with EventsT with EventParticipantsT, EventsRepository] =
    ZLayer.fromServices[Db.Service, EventsTable, EventParticipantsTable, EventsRepository.Service]((d, t, p) => SlickEventsRepository(d, t, p))
}
