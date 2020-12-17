package tevent.organizations.repository

import slick.dbio.DBIO
import tevent.core.Db.{TaskOps, ZIOOps}
import tevent.core.{Db, RepositoryError}
import tevent.organizations.model._
import tevent.organizations.repository.tables.OrgParticipantsTable
import tevent.user.model.User
import zio._

object SlickOrganizationParticipantsRepository {
  def apply(db: Db.Service, participants: OrgParticipantsTable)
  : OrganizationParticipantsRepository.Service = new OrganizationParticipantsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def getParticipants(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType)]] =
      io(participants.getUsersFrom(organizationId)).map(_.toList).refineRepositoryError

    override def check(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationType]] =
      io(participants.checkUserIn(userId, organizationId)).refineRepositoryError

    override def add(participation: OrgParticipation): IO[RepositoryError, Unit] =
      io(participants.add(participation)).unit.refineRepositoryError

    override def update(participation: OrgParticipation): IO[RepositoryError, Unit] =
      io(participants.update(participation)).unit.refineRepositoryError

    override def remove(userId: Long, organizationId: Long): IO[RepositoryError, Unit] =
      io(participants.removeUserFrom(userId, organizationId)).unit.refineRepositoryError
  }


  def live: URLayer[Db with OrgParticipantsT, OrganizationParticipantsRepository] =
    ZLayer.fromServices[Db.Service, OrgParticipantsTable, OrganizationParticipantsRepository.Service](
      SlickOrganizationParticipantsRepository.apply)
}
