package tevent.events.repository

import tevent.core.RepositoryError
import tevent.events.model.{Event, EventFilter, EventParticipationType}
import tevent.organizations.model.PlainOrganization
import zio.IO

object EventsRepository {
  trait Service {
    def add(event: Event): IO[RepositoryError, Long]
    def search(eventFilter: EventFilter): IO[RepositoryError, List[(Event, PlainOrganization)]]
    def getByUser(userId: Long): IO[RepositoryError, List[(Event, EventParticipationType, PlainOrganization)]]
    def getById(id: Long): IO[RepositoryError, Option[(Event, PlainOrganization)]]
    def update(event: Event): IO[RepositoryError, Unit]
  }
}
