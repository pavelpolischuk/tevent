package tevent.domain.repository

import tevent.domain.RepositoryError
import tevent.domain.model.{OrgParticipationType, Organization, User}
import zio.IO

object OrganizationsRepository {
  trait Service {
    def add(organization: Organization): IO[RepositoryError, Long]
    val getAll: IO[RepositoryError, List[Organization]]
    def getByUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType)]]
    def getById(id: Long): IO[RepositoryError, Option[Organization]]
    def update(organization: Organization): IO[RepositoryError, Unit]

    def getUsers(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType)]]
    def checkUser(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationType]]
    def addUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[RepositoryError, Unit]
    def removeUser(userId: Long, organizationId: Long): IO[RepositoryError, Unit]
  }
}
