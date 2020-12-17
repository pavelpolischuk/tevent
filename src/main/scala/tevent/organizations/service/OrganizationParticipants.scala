package tevent.organizations.service

import tevent.core.EntityNotFound.noneToNotFound
import tevent.core.{AccessError, DomainError, ValidationError}
import tevent.organizations.model._
import tevent.organizations.repository.{OrganizationParticipantsRepository, OrganizationRequestsRepository}
import tevent.user.model.User
import zio.{IO, URLayer, ZIO, ZLayer}

object OrganizationParticipants {
  trait Service {
    def checkUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[DomainError, Unit]
    def getUsers(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType)]]
    def getRequests(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType, User)]]
    def getOwnRequests(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]]

    def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit]
    def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): IO[DomainError, Unit]
    def approveRequest(userId: Long, organizationId: Long, approverId: Long): IO[DomainError, Unit]
    def leaveOrganization(userId: Long, organizationId: Long): IO[DomainError, Unit]
  }

  class OrganizationParticipantsImpl(participants: OrganizationParticipantsRepository.Service,
                                     requests: OrganizationRequestsRepository.Service) extends OrganizationParticipants.Service {

    override def checkUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[DomainError, Unit] =
      participants.check(userId, organizationId).flatMap {
        case Some(v) if v >= role => IO.unit
        case _ => IO.fail(AccessError)
      }

    override def getUsers(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType)]] = for {
      _ <- checkUser(userId, organizationId, OrgMember)
      req <- participants.getParticipants(organizationId)
    } yield req


    override def getRequests(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType, User)]] = for {
      _ <- checkUser(userId, organizationId, OrgManager)
      req <- requests.getForOrganization(organizationId)
    } yield req

    override def getOwnRequests(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]] =
      requests.getForUser(userId).map(_.map(r => (r._1, r._2, Option.when(userId != r._3.id)(r._3))))


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

    override def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): IO[DomainError, Unit] = for {
      _ <- checkUser(inviterId, participation.organizationId, participation.participationType)
      _ <- requests.add(OrgParticipationRequest(participation, inviterId))
    } yield ()

    override def approveRequest(userId: Long, organizationId: Long, approverId: Long): IO[DomainError, Unit] = for {
      request <- requests.get(userId, organizationId)
        .flatMap(noneToNotFound((userId, organizationId)))
        .filterOrFail(_.fromUserId == userId)(ValidationError("Request not from user"))
      needApproverRole = if (request.participationType >= OrgManager) request.participationType else OrgManager
      _ <- checkUser(approverId, organizationId, needApproverRole)
      _ <- requests.remove(userId, organizationId)
      _ <- participants.add(request.toParticipation)
    } yield ()

    override def leaveOrganization(userId: Long, organizationId: Long): IO[DomainError, Unit] =
      requests.remove(userId, organizationId).andThen(
        participants.remove(userId, organizationId)
      )
  }


  def live: URLayer[OrganizationParticipantsRepository with OrganizationRequestsRepository, OrganizationParticipants] =
    ZLayer.fromServices[OrganizationParticipantsRepository.Service, OrganizationRequestsRepository.Service, OrganizationParticipants.Service](
      new OrganizationParticipantsImpl(_, _))


  def getUsers(userId: Long, organizationId: Long): ZIO[OrganizationParticipants, DomainError, List[(User, OrgParticipationType)]] =
    ZIO.accessM(_.get.getUsers(userId, organizationId))

  def getRequests(userId: Long, organizationId: Long): ZIO[OrganizationParticipants, DomainError, List[(User, OrgParticipationType, User)]] =
    ZIO.accessM(_.get.getRequests(userId, organizationId))

  def getOwnRequests(userId: Long): ZIO[OrganizationParticipants, DomainError, List[(Organization, OrgParticipationType, Option[User])]] =
    ZIO.accessM(_.get.getOwnRequests(userId))


  def joinOrganization(participation: OrgParticipation): ZIO[OrganizationParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.joinOrganization(participation))

  def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): ZIO[OrganizationParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.inviteIntoOrganization(participation, inviterId))

  def approveRequest(userId: Long, organizationId: Long, approverId: Long): ZIO[OrganizationParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.approveRequest(userId, organizationId, approverId))

  def leaveOrganization(userId: Long, organizationId: Long): ZIO[OrganizationParticipants, DomainError, Unit] =
    ZIO.accessM(_.get.leaveOrganization(userId, organizationId))
}
