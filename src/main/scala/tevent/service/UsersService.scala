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
    def update(id: Long, name: String, email: String): IO[DomainError, Unit]
    def findWithEmail(email: String): IO[DomainError, User]
    def createUser(name: String, email: String, secret: String): IO[DomainError, User]
  }

  class UsersServiceImpl(repository: UsersRepository.Service) extends UsersService.Service {

    override def get(id: Long): IO[DomainError, User] =
      repository.getById(id).flatMap(noneToNotFound(id))

    override def findWithEmail(email: String): IO[DomainError, User] =
      repository.findWithEmail(email).flatMap(noneToNotFound(email))

    override def createUser(name: String, email: String, secret: String): IO[DomainError, User] = {
      repository.findWithEmail(email).flatMap {
        case None =>
          val user = User(id = -1, name = name, email = email, secretHash = secret)
          repository.add(user).map(id => user.copy(id = id))

        case _ => IO.fail(ValidationError(s"User with email <$email> exists"))
      }
    }

    override def update(id: Long, name: String, email: String): IO[DomainError, Unit] = for {
      withNewEmail <- repository.findWithEmail(email)
      _ <- if (withNewEmail.exists(_.id != id)) IO.fail(ValidationError(s"User with email <$email> exists"))
           else repository.update(User(id, name, email, ""))
    } yield ()
  }

  def live: URLayer[UsersRepository, UsersService] = ZLayer.fromService(new UsersServiceImpl(_))

  def update(id: Long, name: String, email: String): ZIO[UsersService, DomainError, Unit] =
    ZIO.accessM(_.get.update(id, name, email))
}
