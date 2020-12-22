package tevent.user.service

import tevent.core.{DomainError, EntityNotFound, ValidationError}
import tevent.user.model.{Email, User, UserAccount, UserId}
import tevent.user.repository.UsersRepository
import zio.{IO, URLayer, ZIO, ZLayer}

object Users {
  trait Service {
    def get(id: UserId): IO[DomainError, User]
    def find(account: UserAccount): IO[DomainError, User]
    def create(name: String, email: String, secret: String): IO[DomainError, User]
    def update(id: UserId, name: String, email: String): IO[DomainError, Unit]
    def remove(id: UserId): IO[DomainError, Unit]
  }

  class UsersServiceImpl(repository: UsersRepository.Service) extends Users.Service {

    override def get(id: UserId): IO[DomainError, User] =
      repository.getById(id.id).someOrFail(EntityNotFound(id))

    override def find(email: UserAccount): IO[DomainError, User] =
      repository.find(email).someOrFail(EntityNotFound(email))

    override def create(name: String, email: String, secret: String): IO[DomainError, User] =
      repository.find(Email(email)).flatMap {
        case None =>
          val user = User(id = -1, name = name, email = email, secretHash = Some(secret), googleId = None, lastRevoke = 0)
          repository.add(user).map(id => user.copy(id = id))

        case _ => IO.fail(ValidationError(s"User with email <$email> exists"))
      }

    override def update(id: UserId, name: String, email: String): IO[DomainError, Unit] =
      repository.find(Email(email)).flatMap {
        case Some(user) if user.id != id.id => IO.fail(ValidationError(s"User with email <$email> exists"))
        case _ => repository.updateInfo(id.id, name, email)
      }

    override def remove(id: UserId): IO[DomainError, Unit] =
      repository.remove(id.id).filterOrFail[DomainError](p => p)(EntityNotFound(id)).unit
  }

  def live: URLayer[UsersRepository, Users] = ZLayer.fromService(new UsersServiceImpl(_))

  def update(id: UserId, name: String, email: String): ZIO[Users, DomainError, Unit] =
    ZIO.accessM(_.get.update(id, name, email))

  def remove(id: UserId): ZIO[Users, DomainError, Unit] =
    ZIO.accessM(_.get.remove(id))
}
