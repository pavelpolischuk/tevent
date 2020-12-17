package tevent.organizations.repository

import slick.dbio.DBIO
import tevent.core.Db.{TaskOps, ZIOOps}
import tevent.core.{Db, RepositoryError}
import tevent.organizations.model._
import tevent.organizations.repository.tables.{OrgParticipationRequestsTable, OrganizationTagsTable}
import tevent.user.model.User
import zio._

object SlickOrganizationRequestsRepository {
  def apply(db: Db.Service, tags: OrganizationTagsTable, requests: OrgParticipationRequestsTable)
  : OrganizationRequestsRepository.Service = new OrganizationRequestsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def get(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationRequest]] =
      io(requests.get(userId, organizationId)).refineRepositoryError

    override def getForOrganization(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType, User)]] =
      io(requests.forOrganization(organizationId)).map(_.toList).refineRepositoryError

    override def getForUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType, User)]] =
      io(requests.forUser(userId)).flatMap(
        ZIO.foreach(_)(req => io(tags.withId(req._1.id)).map(t =>
          (Organization(req._1.id, req._1.name, req._1.nick, req._1.description, t.toList), req._2, req._3)
        )).map(_.toList)
      ).refineRepositoryError

    override def add(request: OrgParticipationRequest): IO[RepositoryError, Unit] =
      io(requests.add(request)).unit.refineRepositoryError

    override def remove(userId: Long, organizationId: Long): IO[RepositoryError, Unit] =
      io(requests.remove(userId, organizationId)).unit.refineRepositoryError

    override def update(request: OrgParticipationRequest): IO[RepositoryError, Unit] =
      io(requests.update(request)).unit.refineRepositoryError
  }


  def live: URLayer[Db with OrganizationTagsT with OrgParticipationRequestsT, OrganizationRequestsRepository] =
    ZLayer.fromServices[Db.Service, OrganizationTagsTable, OrgParticipationRequestsTable, OrganizationRequestsRepository.Service](
      SlickOrganizationRequestsRepository.apply)
}
