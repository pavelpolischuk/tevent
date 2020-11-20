package tevent.service

import tevent.domain.Named.userNamed
import tevent.domain.model.User
import tevent.domain.repository.UsersRepository
import tevent.domain.{DomainError, EntityNotFound, ValidationError}
import zio.{IO, URLayer, ZLayer}

object UsersService {
  trait Service {
    def get(id: Long): IO[DomainError, User]
    def findWithEmail(email: String): IO[DomainError, User]
    def createUser(name: String, email: String, secret: String): IO[DomainError, User]
  }

  class UsersServiceImpl(repository: UsersRepository.Service) extends UsersService.Service {

    override def get(id: Long): IO[DomainError, User] =
      repository.getById(id).flatMap(EntityNotFound.optionToIO(id))

    override def findWithEmail(email: String): IO[DomainError, User] =
      repository.findWithEmail(email).flatMap(EntityNotFound.optionToIO(email))

    override def createUser(name: String, email: String, secret: String): IO[DomainError, User] = {
      repository.findWithEmail(email).flatMap {
        case None =>
          val user = User(id = -1, name = name, email = email, secretHash = secret)
          repository.add(user).map(id => user.copy(id = id))

        case _ => IO.fail(ValidationError(s"User with email <$email> exists"))
      }
    }
  }

  def live: URLayer[UsersRepository, UsersService] = ZLayer.fromService(new UsersServiceImpl(_))
}
