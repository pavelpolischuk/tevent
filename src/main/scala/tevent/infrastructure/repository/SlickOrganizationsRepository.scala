package tevent.infrastructure.repository

import slick.dbio.DBIO
import tevent.domain.RepositoryError
import tevent.domain.model.{OrgParticipation, OrgParticipationRequest, OrgParticipationType, Organization, User}
import tevent.domain.repository.OrganizationsRepository
import tevent.infrastructure.repository.tables.{OrgParticipantsT, OrgParticipantsTable, OrgParticipationRequestsT, OrgParticipationRequestsTable, OrganizationsT, OrganizationsTable}
import zio._

object SlickOrganizationsRepository {
  def apply(db: Db.Service, table: OrganizationsTable, participants: OrgParticipantsTable, requests: OrgParticipationRequestsTable)
  : OrganizationsRepository.Service = new OrganizationsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def add(organization: Organization): IO[RepositoryError, Long] =
      io(table.add(organization)).refineRepositoryError

    override val getAll: IO[RepositoryError, List[Organization]] =
      io(table.all).map(_.toList).refineRepositoryError

    def getByUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType)]] =
      io(participants.forUser(userId)).map(_.toList).refineRepositoryError

    override def getById(id: Long): IO[RepositoryError, Option[Organization]] =
      io(table.withId(id)).refineRepositoryError

    override def update(organization: Organization): IO[RepositoryError, Unit] =
      io(table.update(organization)).unit.refineRepositoryError


    override def getUsers(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType)]] =
      io(participants.getUsersFrom(organizationId)).map(_.toList).refineRepositoryError

    override def checkUser(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationType]] =
      io(participants.checkUserIn(userId, organizationId)).refineRepositoryError

    override def addUser(participation: OrgParticipation): IO[RepositoryError, Unit] =
      io(participants.add(participation)).unit.refineRepositoryError

    override def updateUser(participation: OrgParticipation): IO[RepositoryError, Unit] =
      io(participants.update(participation)).unit.refineRepositoryError

    override def removeUser(userId: Long, organizationId: Long): IO[RepositoryError, Unit] =
      io(participants.removeUserFrom(userId, organizationId)).unit.refineRepositoryError


    override def getRequest(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationRequest]] =
      io(requests.get(userId, organizationId)).refineRepositoryError

    override def getRequests(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType, User)]] =
      io(requests.forOrganization(organizationId)).map(_.toList).refineRepositoryError

    override def getRequestsForUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType, User)]] =
      io(requests.forUser(userId)).map(_.toList).refineRepositoryError

    override def addRequest(request: OrgParticipationRequest): IO[RepositoryError, Unit] =
      io(requests.add(request)).unit.refineRepositoryError

    override def removeRequest(userId: Long, organizationId: Long): IO[RepositoryError, Unit] =
      io(requests.remove(userId, organizationId)).unit.refineRepositoryError

    override def updateRequest(request: OrgParticipationRequest): IO[RepositoryError, Unit] =
      io(requests.update(request)).unit.refineRepositoryError
  }

  def live: URLayer[Db with OrganizationsT with OrgParticipantsT with OrgParticipationRequestsT, OrganizationsRepository] =
    ZLayer.fromServices[Db.Service, OrganizationsTable, OrgParticipantsTable, OrgParticipationRequestsTable, OrganizationsRepository.Service](
      (d, t, p, r) => SlickOrganizationsRepository(d, t, p, r))
}
