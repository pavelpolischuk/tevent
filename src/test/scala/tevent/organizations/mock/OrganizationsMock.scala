package tevent.organizations.mock

import tevent.core.DomainError
import tevent.organizations.model.{OrgParticipationType, Organization, OrganizationFilter, OrganizationId}
import tevent.organizations.service.Organizations
import tevent.user.model.UserId
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object OrganizationsMock extends Mock[Organizations] {
  object Get extends Effect[OrganizationId, DomainError, Organization]
  object Search extends Effect[OrganizationFilter, DomainError, List[Organization]]
  object GetByUser extends Effect[UserId, DomainError, List[(Organization, OrgParticipationType)]]
  object Update extends Effect[(UserId, Organization), DomainError, Unit]
  object Create extends Effect[(UserId, Organization), DomainError, Organization]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[Organizations] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], Organizations] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new Organizations.Service {
        override def get(id: OrganizationId): IO[DomainError, Organization] = proxy(Get, id)
        override def search(filter: OrganizationFilter): IO[DomainError, List[Organization]] = proxy(Search, filter)
        override def getByUser(userId: UserId): IO[DomainError, List[(Organization, OrgParticipationType)]] = proxy(GetByUser, userId)
        override def update(userId: UserId, organization: Organization): IO[DomainError, Unit] = proxy(Update, userId, organization)
        override def create(userId: UserId, organization: Organization): IO[DomainError, Organization] = proxy(Create, userId, organization)
      }
    }
  }
}
