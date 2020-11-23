package tevent.service

import tevent.domain.DomainError
import tevent.domain.model.{OrgManager, OrgOwner, OrgParticipation, Organization}
import tevent.domain.repository.OrganizationsRepository
import zio.{IO, URLayer, ZIO, ZLayer}

object OrganizationsService {
  trait Service {
    def get(id: Long): IO[DomainError, Option[Organization]]
    def update(userId: Long, organization: Organization): IO[DomainError, Unit]
    def create(userId: Long, name: String): IO[DomainError, Organization]
  }

  class OrganizationsServiceImpl(organizations: OrganizationsRepository.Service, participation: ParticipationService.Service) extends OrganizationsService.Service {

    override def get(id: Long): IO[DomainError, Option[Organization]] = organizations.getById(id)

    override def update(userId: Long, organization: Organization): IO[DomainError, Unit] = for {
      _ <- participation.checkUser(userId, organization.id, OrgManager)
      _ <- organizations.update(organization)
    } yield ()

    override def create(userId: Long, name: String): IO[DomainError, Organization] = for {
      org <- IO.succeed(Organization(-1, name))
      orgId <- organizations.add(org)
      _ <- organizations.addUser(OrgParticipation(userId, orgId, OrgOwner))
    } yield org.copy(orgId)
  }

  def live: URLayer[OrganizationsRepository with ParticipationService, OrganizationsService] =
    ZLayer.fromServices[OrganizationsRepository.Service, ParticipationService.Service, OrganizationsService.Service](
      new OrganizationsServiceImpl(_, _))


  def get(id: Long): ZIO[OrganizationsService, DomainError, Option[Organization]] =
    ZIO.accessM(_.get.get(id))

  def update(userId: Long, organization: Organization): ZIO[OrganizationsService, DomainError, Unit] =
    ZIO.accessM(_.get.update(userId, organization))

  def create(userId: Long, name: String): ZIO[OrganizationsService, DomainError, Organization] =
    ZIO.accessM(_.get.create(userId, name))
}
