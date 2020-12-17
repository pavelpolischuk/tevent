package tevent.organizations.repository

import slick.dbio.DBIO
import tevent.core.Db.{TaskOps, ZIOOps}
import tevent.core.{Db, RepositoryError}
import tevent.organizations.model._
import tevent.organizations.repository.tables.{OrgParticipantsTable, OrganizationTagsTable, OrganizationsTable}
import zio._

object SlickOrganizationsRepository {
  def apply(db: Db.Service, organizations: OrganizationsTable, tags: OrganizationTagsTable,
            participants: OrgParticipantsTable)
  : OrganizationsRepository.Service = new OrganizationsRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def add(organization: Organization): IO[RepositoryError, Long] =
      io(organizations.add(organization)).tap(id => io(tags.add(id, organization.tags))).refineRepositoryError

    override val getAll: IO[RepositoryError, List[Organization]] =
      io(organizations.all).flatMap(
        ZIO.foreach(_)(org => io(tags.withId(org.id)).map(t =>
          Organization(org.id, org.name, org.nick, org.description, t.toList)
        )).map(_.toList)
      ).refineRepositoryError

    override def search(filter: OrganizationFilter): IO[RepositoryError, List[Organization]] =
      io(tags.findOrganizations(filter.tags)).flatMap(r =>
        ZIO.foreach(r)(id => for {
          org <- io(organizations.withId(id)).someOrFail(new RuntimeException("There's not organization with id from Tags"))
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
      io(organizations.withId(id)).flatMap {
        case None => IO.none
        case Some(org) => io(tags.withId(org.id)).map(t => Organization(org.id, org.name, org.nick, org.description, t.toList)).option
      }.refineRepositoryError

    override def update(organization: Organization): IO[RepositoryError, Unit] =
      io(organizations.update(organization)).andThen(
        io(tags.update(organization.id, organization.tags))
      ).unit.refineRepositoryError
  }


  def live: URLayer[Db with OrganizationsT with OrganizationTagsT with OrgParticipantsT, OrganizationsRepository] =
    ZLayer.fromServices[Db.Service, OrganizationsTable, OrganizationTagsTable, OrgParticipantsTable, OrganizationsRepository.Service](
      SlickOrganizationsRepository.apply)
}
