package tevent.user.mock

import tevent.core.DomainError
import tevent.user.model.{User, UserAccount, UserId}
import tevent.user.service.Users
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object UsersMock extends Mock[Users] {
  object Get extends Effect[UserId, DomainError, User]
  object Find extends Effect[UserAccount, DomainError, User]
  object Create extends Effect[(String, String, String), DomainError, User]
  object Update extends Effect[(UserId, String, String), DomainError, Unit]
  object Remove extends Effect[UserId, DomainError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[Users] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], Users] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new Users.Service {
        override def get(id: UserId): IO[DomainError, User] = proxy(Get, id)
        override def find(account: UserAccount): IO[DomainError, User] = proxy(Find, account)
        override def create(name: String, email: String, secret: String): IO[DomainError, User] = proxy(Create, name, email, secret)
        override def update(id: UserId, name: String, email: String): IO[DomainError, Unit] = proxy(Update, id, name, email)
        override def remove(id: UserId): IO[DomainError, Unit] = proxy(Remove, id)
      }
    }
  }
}
