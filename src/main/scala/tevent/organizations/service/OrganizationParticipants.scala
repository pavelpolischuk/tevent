package tevent.organizations.service

import tevent.core.{AccessError, DomainError, EntityNotFound, ValidationError}
import tevent.organizations.model.OrgParticipation.OrgParticipationNamed
import tevent.organizations.model._
import tevent.organizations.repository.{OrganizationParticipantsRepository, OrganizationRequestsRepository}
import tevent.user.model.{User, UserId}
import zio.{IO, URLayer, ZIO, ZLayer}

object OrganizationParticipants {
  trait Service {
    def checkUser(userId: UserId, organizationId: OrganizationId, role: OrgParticipationType): IO[DomainError, Unit]
    def getUsers(userId: UserId, organizationId: OrganizationId): IO[DomainError, List[(User, OrgParticipationType)]]
    def getRequests(userId: UserId, organizationId: OrganizationId): IO[DomainError, List[(User, OrgParticipationType, User)]]
    def getOwnRequests(userId: UserId): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]]

    def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit]
    def inviteIntoOrganization(participation: OrgParticipation, inviterId: UserId): IO[DomainError, Unit]
    def approveRequest(userId: UserId, organizationId: OrganizationId, approverId: UserId): IO[DomainError, Unit]
    def leaveOrganization(userId: UserId, organizationId: OrganizationId): IO[DomainError, Unit]
  }

  class OrganizationParticipantsImpl(participants: OrganizationParticipantsRepository.Service,
                                     requests: OrganizationRequestsRepository.Service) extends OrganizationParticipants.Service {

    override def checkUser(userId: UserId, organizationId: OrganizationId, role: OrgParticipationType): IO[DomainError, Unit] =
      participants.check(userId.id, organizationId.id).flatMap {
        case Some(v) if v >= role => IO.unit
        case _ => IO.fail(AccessError)
      }

    override def getUsers(userId: UserId, organizationId: OrganizationId): IO[DomainError, List[(User, OrgParticipationType)]] = for {
      _ <- checkUser(userId, organizationId, OrgMember)
      req <- participants.getParticipants(organizationId.id)
    } yield req


    override def getRequests(userId: UserId, organizationId: OrganizationId): IO[DomainError, List[(User, OrgParticipationType, User)]] = for {
      _ <- checkUser(userId, organizationId, OrgManager)
      req <- requests.getForOrganization(organizationId.id)
    } yield req

    override def getOwnRequests(userId: UserId): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]] =
      requests.getForUser(userId.id).map(_.map(r => (r._1, r._2, Option.when(userId.id != r._3.id)(r._3))))


    override def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit] =
      if (participation.participationType >= OrgManager)
        requests.get(participation.userId, participation.organizationId).flatMap {
          case Some(approved)
            if approved.fromUserId != participation.userId && approved.participationType >= participation.participationType =>
            participants.add(participation).andThen(
              requests.remove(approved.userId, approved.organizationId))

          case _ => requests.add(OrgParticipationRequest(participation))
        }
      else participants.add(participation).andThen(
        requests.remove(participation.userId, participation.organizationId))

    override def inviteIntoOrganization(participation: OrgParticipation, inviterId: UserId): IO[DomainError, Unit] = for {
      _ <- checkUser(inviterId, OrganizationId(participation.organizationId), participation.participationType)
      _ <- requests.add(OrgParticipationRequest(participation, inviterId.id))
    } yield ()

    override def approveRequest(userId: UserId, organizationId: OrganizationId, approverId: UserId): IO[DomainError, Unit] = for {
      request <- requests.get(userId.id, organizationId.id)
        .someOrFail(EntityNotFound((userId, organizationId)))
        .filterOrFail(_.fromUserId == userId.id)(ValidationError("Request not from user"))
      needApproverRole = if (request.participationType >= OrgManager) request.participationType else OrgManager
      _ <- checkUser(approverId, organizationId, needApproverRole)
      _ <- requests.remove(userId.id, organizationId.id)
      _ <- participants.add(request.toParticipation)
    } yield ()

    override def leaveOrganization(userId: UserId, organizationId: OrganizationId): IO[DomainError, Unit] =
      requests.remove(userId.id, organizationId.id).andThen(
        participants.remove(userId.id, organizationId.id)
      )
  }


  def live: URLayer[OrganizationParticipantsRepository with OrganizationRequestsRepository, OrganizationParticipants] =
    ZLayer.fromServices[OrganizationParticipantsRepository.Service, OrganizationRequestsRepository.Service, OrganizationParticipants.Service](
      new OrganizationParticipantsImpl(_, _))


  def getUsers(userId: UserId, organizationId: OrganizationId): ZIO[OrganizationParticipants, DomainError, List[(User, OrgParticipationType)]] =
    ZIO.accessM(_.get.getUsers(userId, organizationId))

  def getRequests(userId: UserId, organizationId: OrganizationId): ZIO[OrganizationParticipants, DomainError, List[(User, OrgParticipationType, User)]] =
    ZIO.accessM(_.get.getRequests(userId, organizationId))

  def getOwnRequests(userId: UserId): ZIO[OrganizationParticipants, DomainError, List[(Organization, OrgParticipationType, Option[User])]] =
    ZIO.accessM(_.get.getOwnRequests(userId))


  def joinOrganization(participation: OrgParticipation): ZIO[OrganizationParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.joinOrganization(participation))

  def inviteIntoOrganization(participation: OrgParticipation, inviterId: UserId): ZIO[OrganizationParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.inviteIntoOrganization(participation, inviterId))

  def approveRequest(userId: UserId, organizationId: OrganizationId, approverId: UserId): ZIO[OrganizationParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.approveRequest(userId, organizationId, approverId))

  def leaveOrganization(userId: UserId, organizationId: OrganizationId): ZIO[OrganizationParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.leaveOrganization(userId, organizationId))
}
