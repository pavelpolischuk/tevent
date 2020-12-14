package tevent.service

import tevent.domain.EntityNotFound.noneToNotFound
import tevent.domain.Named.userNamed
import tevent.domain.model.User
import tevent.domain.repository.UsersRepository
import tevent.domain.{DomainError, ValidationError}
import zio.{IO, URLayer, ZIO, ZLayer}

object UsersService {
  trait Service {
    def get(id: Long): IO[DomainError, User]
    def findWithEmail(email: String): IO[DomainError, User]
    def createUser(name: String, email: String, secret: String): IO[DomainError, User]
    def update(id: Long, name: String, email: String): IO[DomainError, Unit]
  }

  class UsersServiceImpl(repository: UsersRepository.Service) extends UsersService.Service {

    override def get(id: Long): IO[DomainError, User] =
      repository.getById(id).flatMap(noneToNotFound(id))

    override def findWithEmail(email: String): IO[DomainError, User] =
      repository.findWithEmail(email).flatMap(noneToNotFound(email))

    override def createUser(name: String, email: String, secret: String): IO[DomainError, User] =
      repository.findWithEmail(email).flatMap {
        case None =>
          val user = User(id = -1, name = name, email = email, secretHash = secret, lastRevoke = 0)
          repository.add(user).map(id => user.copy(id = id))

        case _ => IO.fail(ValidationError(s"User with email <$email> exists"))
      }

    override def update(id: Long, name: String, email: String): IO[DomainError, Unit] =
      repository.findWithEmail(email).flatMap {
        case Some(user) if user.id != id => IO.fail(ValidationError(s"User with email <$email> exists"))
        case _ => repository.updateInfo(id, name, email)
      }
  }

  def live: URLayer[UsersRepository, UsersService] = ZLayer.fromService(new UsersServiceImpl(_))

  def update(id: Long, name: String, email: String): ZIO[UsersService, DomainError, Unit] =
    ZIO.accessM(_.get.update(id, name, email))
}
