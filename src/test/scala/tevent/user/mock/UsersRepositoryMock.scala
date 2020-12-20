package tevent.user.mock

import tevent.core.RepositoryError
import tevent.user.model.{User, UserAccount}
import tevent.user.repository.UsersRepository
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object UsersRepositoryMock extends Mock[UsersRepository] {
  object Add extends Effect[User, RepositoryError, Long]
  object Remove extends Effect[Long, RepositoryError, Boolean]
  object GetAll extends Effect[Unit, RepositoryError, List[User]]
  object GetById extends Effect[Long, RepositoryError, Option[User]]
  object Find extends Effect[UserAccount, RepositoryError, Option[User]]
  object UpdateInfo extends Effect[(Long, String, String), RepositoryError, Unit]
  object ChangeSecret extends Effect[(Long, String, Long), RepositoryError, Unit]
  object RevokeTokens extends Effect[(Long, Long), RepositoryError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[UsersRepository] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], UsersRepository] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new UsersRepository.Service {
        override def add(user: User): IO[RepositoryError, Long] = proxy(Add, user)
        override def remove(id: Long): IO[RepositoryError, Boolean] = proxy(Remove, id)
        override val getAll: IO[RepositoryError, List[User]] = proxy(GetAll)
        override def getById(id: Long): IO[RepositoryError, Option[User]] = proxy(GetById, id)
        override def find(account: UserAccount): IO[RepositoryError, Option[User]] = proxy(Find, account)
        override def updateInfo(id: Long, name: String, email: String): IO[RepositoryError, Unit] = proxy(UpdateInfo, id, name, email)
        override def changeSecret(id: Long, secret: String, lastRevoke: Long): IO[RepositoryError, Unit] = proxy(ChangeSecret, id, secret, lastRevoke)
        override def revokeTokens(id: Long, lastRevoke: Long): IO[RepositoryError, Unit] = proxy(RevokeTokens, id, lastRevoke)
      }
    }
  }
}
