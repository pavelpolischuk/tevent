package tevent.events.repository

import slick.dbio.DBIO
import tevent.core.Db.{TaskOps, ZIOOps}
import tevent.core.{Db, RepositoryError}
import tevent.events.model.{EventParticipation, EventParticipationType}
import tevent.events.repository.tables.EventParticipantsTable
import tevent.user.model.User
import zio.{IO, Task, URLayer, ZLayer}

object SlickEventParticipantsRepository {
  def apply(db: Db.Service, participants: EventParticipantsTable): EventParticipantsRepository.Service =
    new EventParticipantsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    def getParticipants(eventId: Long): IO[RepositoryError, List[(User, EventParticipationType)]] =
      io(participants.getUsersBy(eventId)).map(_.toList).refineRepositoryError

    def check(userId: Long, eventId: Long): IO[RepositoryError, Option[EventParticipationType]] =
      io(participants.checkUserIn(userId, eventId)).refineRepositoryError

    def add(participation: EventParticipation): IO[RepositoryError, Unit] =
      io(participants.addUserTo(participation)).unit.refineRepositoryError

    def update(participation: EventParticipation): IO[RepositoryError, Unit] =
      io(participants.update(participation)).unit.refineRepositoryError

    def remove(userId: Long, eventId: Long): IO[RepositoryError, Unit] =
      io(participants.removeUserFrom(userId, eventId)).unit.refineRepositoryError
  }

  def live: URLayer[Db with EventParticipantsT, EventParticipantsRepository] =
    ZLayer.fromServices[Db.Service, EventParticipantsTable, EventParticipantsRepository.Service]((d, p) => SlickEventParticipantsRepository(d, p))
}
