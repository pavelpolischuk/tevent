package tevent.mock

import tevent.core.DomainError
import tevent.organizations.model.{OrgParticipationType, Organization, OrganizationFilter}
import tevent.organizations.service.Organizations
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object OrganizationsMock extends Mock[Organizations] {
  object Get extends Effect[Long, DomainError, Organization]
  object Search extends Effect[OrganizationFilter, DomainError, List[Organization]]
  object GetByUser extends Effect[Long, DomainError, List[(Organization, OrgParticipationType)]]
  object Update extends Effect[(Long, Organization), DomainError, Unit]
  object Create extends Effect[(Long, Organization), DomainError, Organization]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[Organizations] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], Organizations] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new Organizations.Service {
        override def get(id: Long): IO[DomainError, Organization] = proxy(Get, id)
        override def search(filter: OrganizationFilter): IO[DomainError, List[Organization]] = proxy(Search, filter)
        override def getByUser(userId: Long): IO[DomainError, List[(Organization, OrgParticipationType)]] = proxy(GetByUser, userId)
        override def update(userId: Long, organization: Organization): IO[DomainError, Unit] = proxy(Update, userId, organization)
        override def create(userId: Long, organization: Organization): IO[DomainError, Organization] = proxy(Create, userId, organization)
      }
    }
  }
}
