package tevent.organizations.mock

import tevent.core.DomainError
import tevent.organizations.model.{OrgParticipation, OrgParticipationType, Organization}
import tevent.organizations.service.OrganizationParticipants
import tevent.user.model.User
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object OrganizationParticipantsMock extends Mock[OrganizationParticipants] {
  object CheckUser extends Effect[(Long, Long, OrgParticipationType), DomainError, Unit]
  object GetUsers extends Effect[(Long, Long), DomainError, List[(User, OrgParticipationType)]]
  object GetRequests extends Effect[(Long, Long), DomainError, List[(User, OrgParticipationType, User)]]
  object GetOwnRequests extends Effect[Long, DomainError, List[(Organization, OrgParticipationType, Option[User])]]
  object JoinOrganization extends Effect[OrgParticipation, DomainError, Unit]
  object InviteIntoOrganization extends Effect[(OrgParticipation, Long), DomainError, Unit]
  object ApproveRequest extends Effect[(Long, Long, Long), DomainError, Unit]
  object LeaveOrganization extends Effect[(Long, Long), DomainError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[OrganizationParticipants] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], OrganizationParticipants] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new OrganizationParticipants.Service {
        override def checkUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[DomainError, Unit] = proxy(CheckUser, userId, organizationId, role)
        override def getUsers(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType)]] = proxy(GetUsers, userId, organizationId)
        override def getRequests(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType, User)]] = proxy(GetRequests, userId, organizationId)
        override def getOwnRequests(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]] = proxy(GetOwnRequests, userId)
        override def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit] = proxy(JoinOrganization, participation)
        override def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): IO[DomainError, Unit] = proxy(InviteIntoOrganization, participation, inviterId)
        override def approveRequest(userId: Long, organizationId: Long, approverId: Long): IO[DomainError, Unit] = proxy(ApproveRequest, userId, organizationId, approverId)
        override def leaveOrganization(userId: Long, organizationId: Long): IO[DomainError, Unit] = proxy(LeaveOrganization, userId, organizationId)
      }
    }
  }
}
