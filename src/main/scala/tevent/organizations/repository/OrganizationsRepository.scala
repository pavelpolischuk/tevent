package tevent.organizations.repository

import tevent.core.RepositoryError
import tevent.organizations.model._
import zio.IO

object OrganizationsRepository {
  trait Service {
    def add(organization: Organization): IO[RepositoryError, Long]
    val getAll: IO[RepositoryError, List[Organization]]
    def search(filter: OrganizationFilter): IO[RepositoryError, List[Organization]]
    def getByUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType)]]
    def getById(id: Long): IO[RepositoryError, Option[Organization]]
    def update(organization: Organization): IO[RepositoryError, Unit]
  }
}
