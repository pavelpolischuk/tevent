package tevent.user.service

import tevent.core.EntityNotFound.noneToNotFound
import tevent.core.{DomainError, EntityNotFound, ValidationError}
import tevent.user.model.{Email, User, UserAccount}
import tevent.user.model.User.userNamed
import tevent.user.repository.UsersRepository
import zio.{IO, URLayer, ZIO, ZLayer}

object Users {
  trait Service {
    def get(id: Long): IO[DomainError, User]
    def find(account: UserAccount): IO[DomainError, User]
    def create(name: String, email: String, secret: String): IO[DomainError, User]
    def update(id: Long, name: String, email: String): IO[DomainError, Unit]
    def remove(id: Long): IO[DomainError, Unit]
  }

  class UsersServiceImpl(repository: UsersRepository.Service) extends Users.Service {

    override def get(id: Long): IO[DomainError, User] =
      repository.getById(id).flatMap(noneToNotFound(id))

    override def find(email: UserAccount): IO[DomainError, User] =
      repository.find(email).flatMap(noneToNotFound(email))

    override def create(name: String, email: String, secret: String): IO[DomainError, User] =
      repository.find(Email(email)).flatMap {
        case None =>
          val user = User(id = -1, name = name, email = email, secretHash = Some(secret), googleId = None, lastRevoke = 0)
          repository.add(user).map(id => user.copy(id = id))

        case _ => IO.fail(ValidationError(s"User with email <$email> exists"))
      }

    override def update(id: Long, name: String, email: String): IO[DomainError, Unit] =
      repository.find(Email(email)).flatMap {
        case Some(user) if user.id != id => IO.fail(ValidationError(s"User with email <$email> exists"))
        case _ => repository.updateInfo(id, name, email)
      }

    override def remove(id: Long): IO[DomainError, Unit] =
      repository.remove(id).filterOrFail[DomainError](p => p)(EntityNotFound[User, Long](id)).unit
  }

  def live: URLayer[UsersRepository, Users] = ZLayer.fromService(new UsersServiceImpl(_))

  def update(id: Long, name: String, email: String): ZIO[Users, DomainError, Unit] =
    ZIO.accessM(_.get.update(id, name, email))

  def remove(id: Long): ZIO[Users, DomainError, Unit] =
    ZIO.accessM(_.get.remove(id))
}
