package tevent.service

import tevent.domain.EntityNotFound.noneToNotFound
import tevent.domain.Named.{orgParticipationRequestNamed, organizationNamed}
import tevent.domain.model._
import tevent.domain.repository.OrganizationsRepository
import tevent.domain.{AccessError, DomainError, EntityNotFound, ValidationError}
import zio.{IO, URLayer, ZIO, ZLayer}

object ParticipationService {
  trait Service {
    def checkUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[DomainError, Unit]
    def getUsers(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType)]]

    def getOrganizations(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType)]]
    def getRequests(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType, User)]]
    def getOwnRequests(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]]

    def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit]
    def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): IO[DomainError, Unit]
    def approveRequestIntoOrganization(userId: Long, organizationId: Long, approverId: Long): IO[DomainError, Unit]
    def leaveOrganization(userId: Long, organizationId: Long): IO[DomainError, Unit]
  }

  class ParticipationServiceImpl(organizations: OrganizationsRepository.Service) extends ParticipationService.Service {
    override def checkUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[DomainError, Unit] =
      organizations.checkUser(userId, organizationId).flatMap {
        case None => IO.fail(EntityNotFound[Organization, Long](organizationId))
        case Some(v) if v >= role => IO.unit
        case _ => IO.fail(AccessError)
      }

    override def getUsers(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType)]] = for {
      _ <- checkUser(userId, organizationId, OrgMember)
      req <- organizations.getUsers(organizationId)
    } yield req

    override def getOrganizations(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType)]] =
      organizations.getByUser(userId)

    override def getRequests(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType, User)]] = for {
      _ <- checkUser(userId, organizationId, OrgManager)
      req <- organizations.getRequests(organizationId)
    } yield req

    override def getOwnRequests(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]] =
      organizations.getRequestsForUser(userId).map(_.map(r => (r._1, r._2, Option.when(userId != r._3.id)(r._3))))


    override def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit] =
      if (participation.participationType >= OrgManager)
        organizations.getRequest(participation.userId, participation.organizationId).flatMap {
          case Some(approved)
            if approved.fromUserId != participation.userId && approved.participationType >= participation.participationType =>
            organizations.addUser(participation).andThen(
              organizations.removeRequest(approved.userId, approved.organizationId))

          case _ => organizations.addRequest(OrgParticipationRequest(participation))
        }
      else organizations.addUser(participation).andThen(
        organizations.removeRequest(participation.userId, participation.organizationId))

    override def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): IO[DomainError, Unit] = for {
      _ <- checkUser(inviterId, participation.organizationId, participation.participationType)
      _ <- organizations.addRequest(OrgParticipationRequest(participation, inviterId))
    } yield ()

    override def approveRequestIntoOrganization(userId: Long, organizationId: Long, approverId: Long): IO[DomainError, Unit] = for {
      request <- organizations.getRequest(userId, organizationId)
        .flatMap(noneToNotFound((userId, organizationId)))
        .filterOrFail(_.fromUserId == userId)(ValidationError("Request not from user"))
      needApproverRole = if (request.participationType >= OrgManager) request.participationType else OrgManager
      _ <- checkUser(approverId, organizationId, needApproverRole)
      _ <- organizations.removeRequest(userId, organizationId)
      _ <- organizations.addUser(request.toParticipation)
    } yield ()

    override def leaveOrganization(userId: Long, organizationId: Long): IO[DomainError, Unit] =
      organizations.removeRequest(userId, organizationId).andThen(
        organizations.removeUser(userId, organizationId)
      )
  }

  def live: URLayer[OrganizationsRepository, ParticipationService] =
    ZLayer.fromService[OrganizationsRepository.Service, ParticipationService.Service](new ParticipationServiceImpl(_))


  def getUsers(userId: Long, organizationId: Long): ZIO[ParticipationService, DomainError, List[(User, OrgParticipationType)]] =
    ZIO.accessM(_.get.getUsers(userId, organizationId))

  def getOrganizations(userId: Long): ZIO[ParticipationService, DomainError, List[(Organization, OrgParticipationType)]] =
    ZIO.accessM(_.get.getOrganizations(userId))

  def getRequests(userId: Long, organizationId: Long): ZIO[ParticipationService, DomainError, List[(User, OrgParticipationType, User)]] =
    ZIO.accessM(_.get.getRequests(userId, organizationId))

  def getOwnRequests(userId: Long): ZIO[ParticipationService, DomainError, List[(Organization, OrgParticipationType, Option[User])]] =
    ZIO.accessM(_.get.getOwnRequests(userId))


  def joinOrganization(participation: OrgParticipation): ZIO[ParticipationService, DomainError, Unit] =
    ZIO.accessM(_.get.joinOrganization(participation))

  def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): ZIO[ParticipationService, DomainError, Unit] =
    ZIO.accessM(_.get.inviteIntoOrganization(participation, inviterId))

  def approveRequestIntoOrganization(userId: Long, organizationId: Long, approverId: Long): ZIO[ParticipationService, DomainError, Unit] =
    ZIO.accessM(_.get.approveRequestIntoOrganization(userId, organizationId, approverId))

  def leaveOrganization(userId: Long, organizationId: Long): ZIO[ParticipationService, DomainError, Unit] =
    ZIO.accessM(_.get.leaveOrganization(userId, organizationId))
}
