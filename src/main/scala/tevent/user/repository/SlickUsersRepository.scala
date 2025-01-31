package tevent.user.repository

import slick.dbio.DBIO
import tevent.core.Db.{TaskOps, ZIOOps}
import tevent.core.{Db, RepositoryError}
import tevent.user.model.{Email, GoogleId, User, UserAccount}
import zio.{IO, Task, URLayer, ZLayer}

object SlickUsersRepository {
  def apply(db: Db.Service, table: UsersTable): UsersRepository.Service = new UsersRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def add(user: User): IO[RepositoryError, Long] =
      io(table.add(user)).refineRepositoryError

    override def remove(id: Long): IO[RepositoryError, Boolean] =
      io(table.remove(id)).map(_ > 0).refineRepositoryError

    override val getAll: IO[RepositoryError, List[User]] =
      io(table.all).map(_.toList).refineRepositoryError

    override def find(account: UserAccount): IO[RepositoryError, Option[User]] = account match {
      case Email(email) => io(table.withEmail(email)).refineRepositoryError
      case GoogleId(id) => io(table.withEmail(id)).refineRepositoryError
    }

    override def getById(id: Long): IO[RepositoryError, Option[User]] =
      io(table.withId(id)).refineRepositoryError

    override def updateInfo(id: Long, name: String, email: String): IO[RepositoryError, Unit] =
      io(table.updateInfo(id, name, email)).unit.refineRepositoryError

    override def changeSecret(id: Long, secret: String, lastRevoke: Long): IO[RepositoryError, Unit] =
      io(table.changeSecret(id, secret, lastRevoke)).unit.refineRepositoryError

    override def revokeTokens(id: Long, lastRevoke: Long): IO[RepositoryError, Unit] =
      io(table.revokeAccess(id, lastRevoke)).unit.refineRepositoryError
  }

  def live: URLayer[Db with UsersT, UsersRepository] =
    ZLayer.fromServices[Db.Service, UsersTable, UsersRepository.Service]((d, t) => SlickUsersRepository(d, t))
}
