package tevent.organizations.mock

import tevent.core.RepositoryError
import tevent.organizations.model.{OrgParticipationType, Organization, OrganizationFilter}
import tevent.organizations.repository.OrganizationsRepository
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object OrganizationsRepositoryMock extends Mock[OrganizationsRepository] {
  object Add extends Effect[Organization, RepositoryError, Long]
  object GetAll extends Effect[Unit, RepositoryError, List[Organization]]
  object Search extends Effect[OrganizationFilter, RepositoryError, List[Organization]]
  object GetByUser extends Effect[Long, RepositoryError, List[(Organization, OrgParticipationType)]]
  object GetById extends Effect[Long, RepositoryError, Option[Organization]]
  object Update extends Effect[Organization, RepositoryError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[OrganizationsRepository] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], OrganizationsRepository] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new OrganizationsRepository.Service {
        override def add(organization: Organization): IO[RepositoryError, Long] = proxy(Add, organization)
        override val getAll: IO[RepositoryError, List[Organization]] = proxy(GetAll)
        override def search(organizationFilter: OrganizationFilter): IO[RepositoryError, List[Organization]] = proxy(Search, organizationFilter)
        override def getByUser(userId: Long): IO[RepositoryError, List[(Organization, OrgParticipationType)]] = proxy(GetByUser, userId)
        override def getById(id: Long): IO[RepositoryError, Option[Organization]] = proxy(GetById, id)
        override def update(organization: Organization): IO[RepositoryError, Unit] = proxy(Update, organization)
      }
    }
  }
}
