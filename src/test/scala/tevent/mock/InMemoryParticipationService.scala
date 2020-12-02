package tevent.mock

import tevent.domain.Named.organizationNamed
import tevent.domain.model.{OrgManager, OrgParticipation, OrgParticipationType, Organization, User}
import tevent.domain.{AccessError, DomainError, EntityNotFound}
import tevent.service.{OrganizationsService, ParticipationService}
import zio._

class InMemoryParticipationService(user: User, organization: Organization,
                                   participations: Ref[List[(User, Organization, OrgParticipationType)]],
                                   requests: Ref[List[(User, Organization, OrgParticipationType, Option[User])]])
  extends ParticipationService.Service {

  override def checkUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[DomainError, Unit] =
    participations.get.filterOrFail(
      _.exists(item => { item._1.id == userId && item._2.id == organizationId && item._3 >= role }))(AccessError).unit

  override def getUsers(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType)]] =
    participations.get.map(_.filter(_._2.id == organizationId).map(p => (p._1, p._3)))

  override def getOrganizations(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType)]] =
    participations.get.map(_.filter(_._1.id == userId).map(p => (p._2, p._3)))

  override def getRequests(userId: Long, organizationId: Long): IO[DomainError, List[(User, OrgParticipationType, User)]] =
    requests.get.map(_.filter(_._2.id == organizationId).map(p => (p._1, p._3, p._4.getOrElse(p._1))))

  override def getOwnRequests(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType, Option[User])]] =
    requests.get.map(_.filter(_._1.id == userId).map(p => (p._2, p._3, p._4)))

  override def joinOrganization(participation: OrgParticipation): IO[DomainError, Unit] = {
    val p = (user.copy(id=participation.userId), organization.copy(id=participation.organizationId), participation.participationType)
    if (participation.participationType == OrgManager) requests.update(s => s :+ (p._1, p._2, p._3, None))
    else participations.update(s => s :+ p)
  }

  override def inviteIntoOrganization(participation: OrgParticipation, inviterId: Long): IO[DomainError, Unit] = {
    val p = (user.copy(id=participation.userId), organization.copy(id=participation.organizationId),
      participation.participationType, Some(user.copy(id=inviterId)))
    requests.update(s => s :+ p)
  }

  override def approveRequestIntoOrganization(userId: Long, organizationId: Long, approverId: Long): IO[DomainError, Unit] = {
    val thisRequest = (p: (User, Organization, OrgParticipationType, Option[User])) => p._1.id == userId && p._2.id == organizationId
    requests.get.map(_.find(thisRequest)).someOrFail(EntityNotFound("")).flatMap(
      p => participations.update(s => s :+ (p._1, p._2, p._3)) >>> requests.update(s => s.filterNot(thisRequest))
    )
  }


  override def leaveOrganization(userId: Long, organizationId: Long): IO[DomainError, Unit] =
    participations.update(store => store.filterNot(p => p._1.id == userId && p._2.id == organizationId)) >>>
      requests.update(store => store.filterNot(p => p._1.id == userId && p._2.id == organizationId))
}

object InMemoryParticipationService {
  def layer(user: User, organization: Organization): ULayer[ParticipationService] = (for {
    ref <- Ref.make(List.empty[(User, Organization, OrgParticipationType)])
    req <- Ref.make(List.empty[(User, Organization, OrgParticipationType, Option[User])])
  } yield new InMemoryParticipationService(user, organization, ref, req)).toLayer
}