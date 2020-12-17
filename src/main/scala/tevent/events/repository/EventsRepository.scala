package tevent.events.repository

import tevent.core.RepositoryError
import tevent.events.model.{Event, EventFilter, EventParticipationType}
import zio.IO

object EventsRepository {
  trait Service {
    def add(event: Event): IO[RepositoryError, Long]
    def search(eventFilter: EventFilter): IO[RepositoryError, List[Event]]
    def getByUser(userId: Long): IO[RepositoryError, List[(Event, EventParticipationType)]]
    def getById(id: Long): IO[RepositoryError, Option[Event]]
    def update(event: Event): IO[RepositoryError, Unit]
  }
}
