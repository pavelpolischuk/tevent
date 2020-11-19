package tevent.service

import tevent.domain.model.User
import tevent.domain.repository.UsersRepository
import tevent.domain.{DomainError, ValidationError}
import zio.{IO, URLayer, ZLayer}

object UsersService {
  trait Service {
    def get(id: Long): IO[DomainError, Option[User]]
    def login(email: String, secret: String): IO[DomainError, Option[User]]
    def signIn(name: String, email: String, secret: String): IO[DomainError, User]
  }

  class UsersServiceImpl(repository: UsersRepository.Service) extends UsersService.Service {

    override def get(id: Long): IO[DomainError, Option[User]] = repository.getById(id)

    override def login(email: String, secret: String): IO[DomainError, Option[User]] =
      repository.findWithEmail(email)

    override def signIn(name: String, email: String, secret: String): IO[DomainError, User] = {
      repository.findWithEmail(email).flatMap {
        case None =>
          val user = User(id = None, name = name, email = email, secretHash = secret)
          repository.add(user).map(id => user.copy(id = Some(id)))

        case _ => IO.fail(ValidationError(s"User with email <$email> exists"))
      }
    }
  }

  def live: URLayer[UsersRepository, UsersService] = ZLayer.fromService(new UsersServiceImpl(_))
}
