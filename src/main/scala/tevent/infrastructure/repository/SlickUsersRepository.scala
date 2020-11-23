package tevent.infrastructure.repository

import slick.dbio.DBIO
import tevent.domain.RepositoryError
import tevent.domain.model.User
import tevent.domain.repository.UsersRepository
import tevent.infrastructure.repository.tables.{UsersT, UsersTable}
import zio.{IO, Task, URLayer, ZIO, ZLayer}

object SlickUsersRepository {
  def apply(db: Db.Service, table: UsersTable): UsersRepository.Service = new UsersRepository.Service {

    private def io[R](action: DBIO[R]): Task[R] = action.toZIO.provide(db)

    override def add(user: User): IO[RepositoryError, Long] =
      io(table.add(user)).refineRepositoryError

    override val getAll: IO[RepositoryError, List[User]] =
      io(table.all).map(_.toList).refineRepositoryError

    override def findWithEmail(email: String): IO[RepositoryError, Option[User]] =
      io(table.withEmail(email)).refineRepositoryError

    override def getById(id: Long): IO[RepositoryError, Option[User]] =
      io(table.withId(id)).refineRepositoryError

    override def update(user: User): IO[RepositoryError, Unit] =
      io(table.update(user)).unit.refineRepositoryError
  }

  def live: URLayer[Db with UsersT, UsersRepository] =
    ZLayer.fromServices[Db.Service, UsersTable, UsersRepository.Service]((d, t) => SlickUsersRepository(d, t))
}
