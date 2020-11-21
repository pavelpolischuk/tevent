package tevent.infrastructure.repository

import slick.dbio.DBIO
import tevent.domain.RepositoryError
import tevent.domain.model.{OrgParticipationType, Organization, User}
import tevent.domain.repository.OrganizationsRepository
import tevent.infrastructure.repository.tables.{OrgParticipantsT, OrgParticipantsTable, OrganizationsT, OrganizationsTable}
import zio._

object SlickOrganizationsRepository {
  def apply(db: Db.Service, table: OrganizationsTable, participants: OrgParticipantsTable): OrganizationsRepository.Service = new OrganizationsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = ZIO.fromDBIO(action).provide(db)

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

    override def addUser(userId: Long, organizationId: Long, role: OrgParticipationType): IO[RepositoryError, Unit] =
      io(participants.addUserTo(userId, organizationId, role)).unit.refineRepositoryError

    override def removeUser(userId: Long, organizationId: Long): IO[RepositoryError, Unit] =
      io(participants.removeUserFrom(userId, organizationId)).unit.refineRepositoryError
  }

  def live: URLayer[Db with OrganizationsT with OrgParticipantsT, OrganizationsRepository] =
    ZLayer.fromServices[Db.Service, OrganizationsTable, OrgParticipantsTable, OrganizationsRepository.Service]((d, t, p) => SlickOrganizationsRepository(d, t, p))
}
