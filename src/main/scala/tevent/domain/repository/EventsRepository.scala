package tevent.domain.repository

import tevent.domain.RepositoryError
import tevent.domain.model.{Event, EventFilter, EventParticipation, EventParticipationType, User}
import zio.IO

object EventsRepository {
  trait Service {
    def add(event: Event): IO[RepositoryError, Long]
    def search(eventFilter: EventFilter): IO[RepositoryError, List[Event]]
    def getByUser(userId: Long): IO[RepositoryError, List[(Event, EventParticipationType)]]
    def getById(id: Long): IO[RepositoryError, Option[Event]]
    def update(event: Event): IO[RepositoryError, Unit]

    def getUsers(eventId: Long): IO[RepositoryError, List[(User, EventParticipationType)]]
    def checkUser(userId: Long, eventId: Long): IO[RepositoryError, Option[EventParticipationType]]
    def addUser(participation: EventParticipation): IO[RepositoryError, Unit]
    def updateUser(participation: EventParticipation): IO[RepositoryError, Unit]
    def removeUser(userId: Long, eventId: Long): IO[RepositoryError, Unit]
  }
}
