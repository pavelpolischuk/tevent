package tevent.organizations.mock

import tevent.core.DomainError
import tevent.organizations.model.{OrgParticipation, OrgParticipationType, Organization, OrganizationId}
import tevent.organizations.service.OrganizationParticipants
import tevent.user.model.{User, UserId}
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object OrganizationParticipantsMock extends Mock[OrganizationParticipants] {
  object CheckUser extends Effect[(UserId, OrganizationId, OrgParticipationType), DomainError, Unit]
  object GetUsers extends Effect[(UserId, OrganizationId), DomainError, List[(User, OrgParticipationType)]]
  object GetRequests extends Effect[(UserId, OrganizationId), DomainError, List[(User, OrgParticipationType, User)]]
  object GetOwnRequests extends Effect[UserId, DomainError, List[(Organization, OrgParticipationType, Option[User])]]
  object JoinOrganization extends Effect[OrgParticipation, DomainError, Unit]
  object InviteIntoOrganization extends Effect[(OrgParticipation, UserId), DomainError, Unit]
  object ApproveRequest extends Effect[(UserId, OrganizationId, UserId), DomainError, Unit]
  object LeaveOrganization extends Effect[(UserId, OrganizationId), DomainError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[OrganizationParticipants] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], OrganizationParticipants] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new OrganizationParticipants.Service {
        override def checkUser(userId: UserId, organizationId: OrganizationId, role: OrgParticipationType): IO[DomainError, Unit] = proxy(CheckUser, userId, organizationId, role)
        override def getUsers(userId: UserId, organizationId: OrganizationId): IO[DomainError, List[(User, OrgParticipationType)]] = proxy(GetUsers, userId, organizationId)
        override def getRequests(userId: UserId, organizationId: OrganizationId): IO[DomainError, List[(User, OrgParticipationType, User)]] = proxy(GetRequests, userId, organizationId)
        override def getOwnRequests(userId: UserId): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]] = proxy(GetOwnRequests, userId)
        override def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit] = proxy(JoinOrganization, participation)
        override def inviteIntoOrganization(participation: OrgParticipation, inviterId: UserId): IO[DomainError, Unit] = proxy(InviteIntoOrganization, participation, inviterId)
        override def approveRequest(userId: UserId, organizationId: OrganizationId, approverId: UserId): IO[DomainError, Unit] = proxy(ApproveRequest, userId, organizationId, approverId)
        override def leaveOrganization(userId: UserId, organizationId: OrganizationId): IO[DomainError, Unit] = proxy(LeaveOrganization, userId, organizationId)
      }
    }
  }
}
