package tevent.domain.repository

import tevent.domain.RepositoryError
import tevent.domain.model.User
import zio.IO

object UsersRepository {
  trait Service {
    def add(user: User): IO[RepositoryError, Long]
    val getAll: IO[RepositoryError, List[User]]
    def getById(id: Long): IO[RepositoryError, Option[User]]
    def findWithEmail(email: String): IO[RepositoryError, Option[User]]

    def updateInfo(id: Long, name: String, email: String): IO[RepositoryError, Unit]
    def changeSecret(id: Long, secret: String, lastRevoke: Long): IO[RepositoryError, Unit]
    def revokeTokens(id: Long, lastRevoke: Long): IO[RepositoryError, Unit]
  }
}
