package tevent.organizations.mock

import tevent.core.RepositoryError
import tevent.organizations.model.{OrgParticipation, OrgParticipationType}
import tevent.organizations.repository.OrganizationParticipantsRepository
import tevent.user.model.User
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object OrganizationParticipantsRepositoryMock extends Mock[OrganizationParticipantsRepository] {
  object GetParticipants extends Effect[Long, RepositoryError, List[(User, OrgParticipationType)]]
  object Check extends Effect[(Long, Long), RepositoryError, Option[OrgParticipationType]]
  object Add extends Effect[OrgParticipation, RepositoryError, Unit]
  object Update extends Effect[OrgParticipation, RepositoryError, Unit]
  object Remove extends Effect[(Long, Long), RepositoryError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[OrganizationParticipantsRepository] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], OrganizationParticipantsRepository] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new OrganizationParticipantsRepository.Service {
        override def getParticipants(organizationId: Long): IO[RepositoryError, List[(User, OrgParticipationType)]] = proxy(GetParticipants, organizationId)
        override def check(userId: Long, organizationId: Long): IO[RepositoryError, Option[OrgParticipationType]] = proxy(Check, userId, organizationId)
        override def add(participation: OrgParticipation): IO[RepositoryError, Unit] = proxy(Add, participation)
        override def update(participation: OrgParticipation): IO[RepositoryError, Unit] = proxy(Update, participation)
        override def remove(userId: Long, organizationId: Long): IO[RepositoryError, Unit] = proxy(Remove, userId, organizationId)
      }
    }
  }
}
