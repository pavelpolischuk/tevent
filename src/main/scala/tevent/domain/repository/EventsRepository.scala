package tevent.domain.repository

import tevent.domain.RepositoryError
import tevent.domain.model.Event
import zio.IO

object EventsRepository {
  trait Service {
    def add(event: Event): IO[RepositoryError, Long]
    val getAll: IO[RepositoryError, List[Event]]
    def getByUser(userId: Long): IO[RepositoryError, List[Event]]
    def getByOrganization(organizationId: Long): IO[RepositoryError, List[Event]]
    def getById(id: Long): IO[RepositoryError, Option[Event]]
    def update(event: Event): IO[RepositoryError, Unit]
  }
}
