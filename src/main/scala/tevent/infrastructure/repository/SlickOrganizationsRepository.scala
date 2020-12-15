package tevent.infrastructure.repository

import slick.dbio.DBIO
import tevent.domain.RepositoryError
import tevent.domain.model._
import tevent.domain.repository.OrganizationsRepository
import tevent.infrastructure.repository.tables.{OrgParticipantsT, OrgParticipantsTable, OrgParticipationRequestsT, OrgParticipationRequestsTable, OrganizationTagsT, OrganizationTagsTable, OrganizationsT, OrganizationsTable}
import zio._

object SlickOrganizationsRepository {
  def apply(db: Db.Service, table: OrganizationsTable, tags: OrganizationTagsTable,
            participants: OrgParticipantsTable, requests: OrgParticipationRequestsTable)
  : OrganizationsRepository.Service = new OrganizationsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def add(organization: Organization): IO[RepositoryError, Long] =
      io(table.add(organization)).tap(id => io(tags.add(id, organization.tags))).refineRepositoryError

    override val getAll: IO[RepositoryError, List[Organization]] =
      io(table.all).flatMap(
        ZIO.foreach(_)(org => io(tags.withId(org.id)).map(t =>
          Organization(org.id, org.name, org.nick, org.description, t.toList)
        )).map(_.toList)
      ).refineRepositoryError

    override def search(filter: OrganizationFilter): IO[RepositoryError, List[Organization]] =
      io(tags.findOrganizations(filter.tags)).flatMap(r =>
        ZIO.foreach(r)(id => for {
          org <- io(table.withId(id)).someOrFail(new RuntimeException("There's not organization with id from Tags"))
          tags <- io(tags.withId(id))
        } yield Organization(id, org.name, org.nick, org.description, tags.toList)).map(_.toList)
      ).refineRepositoryError

    override def getByUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType)]] =
      io(participants.forUser(userId)).flatMap(
        ZIO.foreach(_)(org => io(tags.withId(org._1.id)).map(t =>
          (Organization(org._1.id, org._1.name, org._1.nick, org._1.description, t.toList), org._2)
        )).map(_.toList)
      ).refineRepositoryError

    override def getById(id: Long): IO[RepositoryError, Option[Organization]] =
      io(table.withId(id)).flatMap {
        case None => IO.none
        case Some(org) => io(tags.withId(org.id)).map(t => Organization(org.id, org.name, org.nick, org.description, t.toList)).option
      }.refineRepositoryError

    override def update(organization: Organization): IO[RepositoryError, Unit] =
      io(table.update(organization)).andThen(
        io(tags.update(organization.id, organization.tags))
      ).unit.refineRepositoryError

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
      io(requests.forUser(userId)).flatMap(
        ZIO.foreach(_)(req => io(tags.withId(req._1.id)).map(t =>
          (Organization(req._1.id, req._1.name, req._1.nick, req._1.description, t.toList), req._2, req._3)
        )).map(_.toList)
      ).refineRepositoryError

    override def addRequest(request: OrgParticipationRequest): IO[RepositoryError, Unit] =
      io(requests.add(request)).unit.refineRepositoryError

    override def removeRequest(userId: Long, organizationId: Long): IO[RepositoryError, Unit] =
      io(requests.remove(userId, organizationId)).unit.refineRepositoryError

    override def updateRequest(request: OrgParticipationRequest): IO[RepositoryError, Unit] =
      io(requests.update(request)).unit.refineRepositoryError
  }

  def live: URLayer[Db with OrganizationsT with OrganizationTagsT with OrgParticipantsT with OrgParticipationRequestsT, OrganizationsRepository] =
    ZLayer.fromServices[Db.Service, OrganizationsTable, OrganizationTagsTable, OrgParticipantsTable, OrgParticipationRequestsTable, OrganizationsRepository.Service](
      (d, t, g, p, r) => SlickOrganizationsRepository(d, t, g, p, r))
}
