package tevent.organizations.repository

import tevent.core.RepositoryError
import tevent.organizations.model._
import tevent.user.model.User
import zio.IO

object OrganizationsRepository {
  trait Service {
    def add(organization: Organization): IO[RepositoryError, Long]

    val getAll: IO[RepositoryError, List[Organization]]

    def search(filter: OrganizationFilter): IO[RepositoryError, List[Organization]]

    def getByUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType)]]

    def getById(id: Long): IO[RepositoryError, Option[Organization]]

    def update(organization: Organization): IO[RepositoryError, Unit]

    def getUsers(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType)]]

    def checkUser(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationType]]

    def addUser(participation: OrgParticipation): IO[RepositoryError, Unit]

    def updateUser(participation: OrgParticipation): IO[RepositoryError, Unit]

    def removeUser(userId: Long, organizationId: Long): IO[RepositoryError, Unit]

    def getRequest(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationRequest]]

    def getRequests(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType, User)]]

    def getRequestsForUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType, User)]]

    def addRequest(request: OrgParticipationRequest): IO[RepositoryError, Unit]

    def removeRequest(userId: Long, organizationId: Long): IO[RepositoryError, Unit]

    def updateRequest(request: OrgParticipationRequest): IO[RepositoryError, Unit]
  }
}
