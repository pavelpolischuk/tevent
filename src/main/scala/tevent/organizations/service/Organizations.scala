package tevent.organizations.service

import tevent.core.DomainError
import tevent.organizations.model.{OrgManager, OrgOwner, OrgParticipation, Organization, OrganizationFilter}
import tevent.organizations.repository.OrganizationsRepository
import zio.{IO, URLayer, ZIO, ZLayer}

object Organizations {
  trait Service {
    def get(id: Long): IO[DomainError, Option[Organization]]
    def search(filter: OrganizationFilter): IO[DomainError, List[Organization]]
    def update(userId: Long, organization: Organization): IO[DomainError, Unit]
    def create(userId: Long, organization: Organization): IO[DomainError, Organization]
  }

  class OrganizationsServiceImpl(organizations: OrganizationsRepository.Service, participation: OrganizationParticipants.Service) extends Organizations.Service {

    override def get(id: Long): IO[DomainError, Option[Organization]] = organizations.getById(id)

    override def search(filter: OrganizationFilter): IO[DomainError, List[Organization]] =
      if (filter.isEmpty) organizations.getAll
      else organizations.search(filter)

    override def update(userId: Long, organization: Organization): IO[DomainError, Unit] = for {
      _ <- participation.checkUser(userId, organization.id, OrgManager)
      _ <- organizations.update(organization)
    } yield ()

    override def create(userId: Long, organization: Organization): IO[DomainError, Organization] = for {
      orgId <- organizations.add(organization)
      _ <- organizations.addUser(OrgParticipation(userId, orgId, OrgOwner))
    } yield organization.copy(id = orgId)
  }

  def live: URLayer[OrganizationsRepository with OrganizationParticipants, Organizations] =
    ZLayer.fromServices[OrganizationsRepository.Service, OrganizationParticipants.Service, Organizations.Service](
      new OrganizationsServiceImpl(_, _))


  def get(id: Long): ZIO[Organizations, DomainError, Option[Organization]] =
    ZIO.accessM(_.get.get(id))

  def search(filter: OrganizationFilter): ZIO[Organizations, DomainError, List[Organization]] =
    ZIO.accessM(_.get.search(filter))

  def update(userId: Long, organization: Organization): ZIO[Organizations, DomainError, Unit] =
    ZIO.accessM(_.get.update(userId, organization))

  def create(userId: Long, organization: Organization): ZIO[Organizations, DomainError, Organization] =
    ZIO.accessM(_.get.create(userId, organization))
}
