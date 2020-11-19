package tevent.domain.repository

import tevent.domain.RepositoryError
import tevent.domain.model.Organization
import zio.IO

object OrganizationsRepository {
  trait Service {
    def add(organization: Organization): IO[RepositoryError, Long]
    val getAll: IO[RepositoryError, List[Organization]]
    def getByUser(userId: Long): IO[RepositoryError, List[Organization]]
    def getById(id: Long): IO[RepositoryError, Option[Organization]]
    def update(organization: Organization): IO[RepositoryError, Unit]
  }
}
