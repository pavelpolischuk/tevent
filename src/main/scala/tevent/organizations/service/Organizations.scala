package tevent.organizations.service

import tevent.core.{AccessError, DomainError, EntityNotFound}
import tevent.organizations.model._
import tevent.organizations.repository.{OrganizationParticipantsRepository, OrganizationsRepository}
import tevent.user.model.UserId
import zio.{IO, URLayer, ZIO, ZLayer}

object Organizations {
  trait Service {
    def get(id: OrganizationId): IO[DomainError, Organization]
    def search(filter: OrganizationFilter): IO[DomainError, List[Organization]]
    def getByUser(userId: UserId): IO[DomainError, List[(Organization, OrgParticipationType)]]
    def update(userId: UserId, organization: Organization): IO[DomainError, Unit]
    def create(userId: UserId, organization: Organization): IO[DomainError, Organization]
  }

  class OrganizationsServiceImpl(organizations: OrganizationsRepository.Service,
                                 participants: OrganizationParticipantsRepository.Service) extends Organizations.Service {

    override def get(id: OrganizationId): IO[DomainError, Organization] =
      organizations.getById(id.id).someOrFail(EntityNotFound(id))

    override def search(filter: OrganizationFilter): IO[DomainError, List[Organization]] =
      if (filter.isEmpty) organizations.getAll
      else organizations.search(filter)

    override def getByUser(userId: UserId): IO[DomainError, List[(Organization, OrgParticipationType)]] =
      organizations.getByUser(userId.id)

    override def update(userId: UserId, organization: Organization): IO[DomainError, Unit] = for {
      _ <- participants.check(userId.id, organization.id).flatMap {
        case Some(v) if v >= OrgManager => IO.unit
        case _ => IO.fail(AccessError)
      }
      _ <- organizations.update(organization)
    } yield ()

    override def create(userId: UserId, organization: Organization): IO[DomainError, Organization] = for {
      orgId <- organizations.add(organization)
      _ <- participants.add(OrgParticipation(userId.id, orgId, OrgOwner))
    } yield organization.copy(id = orgId)
  }


  def live: URLayer[OrganizationsRepository with OrganizationParticipantsRepository, Organizations] =
    ZLayer.fromServices[OrganizationsRepository.Service, OrganizationParticipantsRepository.Service, Organizations.Service](
      new OrganizationsServiceImpl(_, _))


  def get(id: OrganizationId): ZIO[Organizations, DomainError, Organization] =
    ZIO.accessM(_.get.get(id))

  def search(filter: OrganizationFilter): ZIO[Organizations, DomainError, List[Organization]] =
    ZIO.accessM(_.get.search(filter))

  def getByUser(userId: UserId): ZIO[Organizations, DomainError, List[(Organization, OrgParticipationType)]] =
    ZIO.accessM(_.get.getByUser(userId))

  def update(userId: UserId, organization: Organization): ZIO[Organizations, DomainError, Unit] =
    ZIO.accessM(_.get.update(userId, organization))

  def create(userId: UserId, organization: Organization): ZIO[Organizations, DomainError, Organization] =
    ZIO.accessM(_.get.create(userId, organization))
}
