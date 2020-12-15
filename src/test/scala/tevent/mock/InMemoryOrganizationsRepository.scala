package tevent.mock

import tevent.core.RepositoryError
import tevent.organizations.model._
import tevent.organizations.repository.OrganizationsRepository
import tevent.user.model.User
import zio._

class InMemoryOrganizationsRepository(ref: Ref[Map[Long, Organization]],
                                      users: Ref[Map[Long, List[(User, OrgParticipationType)]]])
  extends OrganizationsRepository.Service {

  override def add(organization: Organization): IO[RepositoryError, Long] = ???

  override val getAll: IO[RepositoryError, List[Organization]] =
    ref.get.map(_.values.toList)

  override def getByUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType)]] = ???

  override def getById(id: Long): IO[RepositoryError, Option[Organization]] =
    ref.get.map(r => r.get(id))

  override def update(organization: Organization): IO[RepositoryError, Unit] = ???

  override def getUsers(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType)]] =
    users.get.map(r => r.getOrElse(organizationId, List.empty))

  override def checkUser(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationType]] = ???

  override def addUser(participation: OrgParticipation): IO[RepositoryError, Unit] = ???

  override def updateUser(participation: OrgParticipation): IO[RepositoryError, Unit] = ???

  override def removeUser(userId: Long, organizationId: Long): IO[RepositoryError, Unit] = ???

  override def getRequest(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationRequest]] = ???

  override def getRequests(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType, User)]] = ???

  override def getRequestsForUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType, User)]] = ???

  override def addRequest(request: OrgParticipationRequest): IO[RepositoryError, Unit] = ???

  override def removeRequest(userId: Long, organizationId: Long): IO[RepositoryError, Unit] = ???

  override def updateRequest(request: OrgParticipationRequest): IO[RepositoryError, Unit] = ???

  override def search(filter: OrganizationFilter): IO[RepositoryError, List[Organization]] = ???
}


object InMemoryOrganizationsRepository {
  def layer(organizations: Ref[Map[Long, Organization]],
            users: Ref[Map[Long, List[(User, OrgParticipationType)]]]): ULayer[OrganizationsRepository] =
    ZLayer.succeed(new InMemoryOrganizationsRepository(organizations, users))
}
