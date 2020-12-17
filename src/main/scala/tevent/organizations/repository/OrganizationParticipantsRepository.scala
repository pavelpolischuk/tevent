package tevent.organizations.repository

import tevent.core.RepositoryError
import tevent.organizations.model._
import tevent.user.model.User
import zio.IO

object OrganizationParticipantsRepository {
  trait Service {
    def getParticipants(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType)]]
    def check(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationType]]
    def add(participation: OrgParticipation): IO[RepositoryError, Unit]
    def update(participation: OrgParticipation): IO[RepositoryError, Unit]
    def remove(userId: Long, organizationId: Long): IO[RepositoryError, Unit]
  }
}
