package tevent.organizations.repository

import tevent.core.RepositoryError
import tevent.organizations.model._
import tevent.user.model.User
import zio.IO

object OrganizationRequestsRepository {
  trait Service {
    def get(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationRequest]]
    def getForOrganization(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType, User)]]
    def getForUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType, User)]]
    def add(request: OrgParticipationRequest): IO[RepositoryError, Unit]
    def remove(userId: Long, organizationId: Long): IO[RepositoryError, Unit]
    def update(request: OrgParticipationRequest): IO[RepositoryError, Unit]
  }
}
