package tevent.domain.repository

import tevent.domain.RepositoryError
import tevent.domain.model.{Event, EventParticipationType, User}
import zio.IO

object EventsRepository {
  trait Service {
    def add(event: Event): IO[RepositoryError, Long]
    val getAll: IO[RepositoryError, List[Event]]
    def getByOrganization(organizationId: Long): IO[RepositoryError, List[Event]]
    def getByUser(userId: Long): IO[RepositoryError, List[(Event, EventParticipationType)]]
    def getById(id: Long): IO[RepositoryError, Option[Event]]
    def update(event: Event): IO[RepositoryError, Unit]

    def getUsers(organizationId: Long): IO[RepositoryError, List[(User, EventParticipationType)]]
    def checkUser(userId: Long, organizationId: Long): IO[RepositoryError, Option[EventParticipationType]]
    def addUser(userId: Long, organizationId: Long, role: EventParticipationType): IO[RepositoryError, Unit]
    def removeUser(userId: Long, organizationId: Long): IO[RepositoryError, Unit]
  }
}
