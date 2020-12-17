package tevent.events.repository

import tevent.core.RepositoryError
import tevent.events.model.{EventParticipation, EventParticipationType}
import tevent.user.model.User
import zio.IO

object EventParticipantsRepository {
  trait Service {
    def getParticipants(eventId: Long): IO[RepositoryError, List[(User, EventParticipationType)]]
    def check(userId: Long, eventId: Long): IO[RepositoryError, Option[EventParticipationType]]
    def add(participation: EventParticipation): IO[RepositoryError, Unit]
    def update(participation: EventParticipation): IO[RepositoryError, Unit]
    def remove(userId: Long, eventId: Long): IO[RepositoryError, Unit]
  }
}
