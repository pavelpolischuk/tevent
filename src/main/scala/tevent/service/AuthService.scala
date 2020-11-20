package tevent.service

import tevent.domain.model.User
import tevent.domain.{DomainError, ValidationError}
import tevent.infrastructure.service.Crypto
import zio.clock.Clock
import zio.{IO, URLayer, ZIO, ZLayer}

import scala.util.Try

object AuthService {
  trait Service {
    def login(email: String, secret: String, nonce: String): IO[DomainError, String]
    def signIn(name: String, email: String, secret: String, nonce: String): IO[DomainError, String]
    def validateUser(token: String): IO[DomainError, User]
  }

  val live: URLayer[Crypto with UsersService, AuthService] = ZLayer.fromServices[Crypto.Service, UsersService.Service, Service] { (c, u) =>
    new Service {
      override def login(email: String, secret: String, nonce: String): IO[DomainError, String] = for {
        user <- u.findWithEmail(email)
        verified <- c.verifySecret(secret, user.secretHash)
        token <- if (verified) c.signToken(user.id.toString, nonce) else IO.fail(ValidationError("Bad secret"))
      } yield token

      override def signIn(name: String, email: String, secret: String, nonce: String): IO[DomainError, String] = for {
        secret <- c.dbSecret(secret)
        user <- u.createUser(name, email, secret)
        token <- c.signToken(user.id.toString, nonce)
      } yield token

      override def validateUser(token: String): IO[DomainError, User] = for {
        idPart <- c.validateSignedToken(token)
        userId <- ZIO.fromTry(Try(idPart.toLong))
          .mapError[DomainError](_ => ValidationError("Invalid token format"))
        user <- u.get(userId)
      } yield user
    }
  }

  def login(email: String, secret: String): ZIO[AuthService with Clock, DomainError, String] =
    ZIO.accessM { e =>
      for {
        nonce <- e.get[Clock.Service].nanoTime
        token <- e.get[AuthService.Service].login(email, secret, nonce.toString)
      } yield token
    }

  def signIn(name: String, email: String, secret: String): ZIO[AuthService with Clock, DomainError, String] =
    ZIO.accessM { e =>
      for {
        nonce <- e.get[Clock.Service].nanoTime
        token <- e.get[AuthService.Service].signIn(name, email, secret, nonce.toString)
      } yield token
    }

  def validateUser(token: String): ZIO[AuthService, DomainError, User] =
    ZIO.accessM(_.get.validateUser(token))
}
