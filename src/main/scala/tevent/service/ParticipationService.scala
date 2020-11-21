package tevent.service

import tevent.domain.Named.organizationNamed
import tevent.domain.model.{Event, EventParticipationType, OrgParticipationType, Organization}
import tevent.domain.repository.{EventsRepository, OrganizationsRepository}
import tevent.domain.{AccessError, DomainError, EntityNotFound}
import zio.{IO, URLayer, ZIO, ZLayer}

object ParticipationService {
  trait Service {
    def getOrganizations(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType)]]
    def getEvents(userId: Long): IO[DomainError, List[(Event, EventParticipationType)]]
    def checkUser(userId: Long, organizationId: Long, roles: Set[OrgParticipationType]): IO[DomainError, Unit]
  }

  class ParticipationServiceImpl(organizations: OrganizationsRepository.Service, events: EventsRepository.Service) extends ParticipationService.Service {
    override def checkUser(userId: Long, organizationId: Long, roles: Set[OrgParticipationType]): IO[DomainError, Unit] =
      organizations.checkUser(userId, organizationId).flatMap {
        case None => IO.fail(EntityNotFound[Organization, Long](organizationId))
        case Some(v) if roles.contains(v) => IO.unit
        case _ => IO.fail(AccessError)
      }

    override def getOrganizations(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType)]] =
      organizations.getByUser(userId)

    override def getEvents(userId: Long): IO[DomainError, List[(Event, EventParticipationType)]] =
      events.getByUser(userId)
  }

  def live: URLayer[OrganizationsRepository with EventsRepository, ParticipationService] =
    ZLayer.fromServices[OrganizationsRepository.Service, EventsRepository.Service, ParticipationService.Service](
      new ParticipationServiceImpl(_, _))


  def getOrganizations(userId: Long): ZIO[ParticipationService, DomainError, List[(Organization, OrgParticipationType)]] =
    ZIO.accessM(_.get.getOrganizations(userId))

  def getEvents(userId: Long): ZIO[ParticipationService, DomainError, List[(Event, EventParticipationType)]] =
    ZIO.accessM(_.get.getEvents(userId))
}
