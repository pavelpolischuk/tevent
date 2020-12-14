package tevent.service

import tevent.domain.model.{User, UserToken}
import tevent.domain.repository.UsersRepository
import tevent.domain.{DomainError, ValidationError}
import tevent.infrastructure.service.Crypto
import zio.clock.Clock
import zio.{IO, URLayer, ZIO, ZLayer}

object AuthService {
  trait Service {
    def login(email: String, secret: String): IO[DomainError, UserToken]
    def signIn(name: String, email: String, secret: String): IO[DomainError, UserToken]
    def validateUser(token: String): IO[DomainError, User]
    def changeSecret(id: Long, secret: String): IO[DomainError, UserToken]
    def revokeTokens(id: Long): IO[DomainError, Unit]
  }

  val live: URLayer[Clock with Crypto with UsersService with UsersRepository, AuthService] = ZLayer.fromServices[Clock.Service, Crypto.Service, UsersService.Service, UsersRepository.Service, Service] { (clock, crypto, users, repo) =>
    new Service {
      override def login(email: String, secret: String): IO[DomainError, UserToken] = for {
        user <- users.findWithEmail(email)
        _ <- crypto.verifySecret(secret, user.secretHash)
          .filterOrFail[DomainError](a => a)(ValidationError("Bad secret"))
        issueTime <- clock.nanoTime
        token <- crypto.getSignedToken(user.id, issueTime)
      } yield token

      override def signIn(name: String, email: String, secret: String): IO[DomainError, UserToken] = for {
        secret <- crypto.dbSecret(secret)
        user <- users.createUser(name, email, secret)
        issueTime <- clock.nanoTime
        token <- crypto.getSignedToken(user.id, issueTime)
      } yield token

      override def validateUser(token: String): IO[DomainError, User] = for {
        token <- crypto.validateSignedToken(token)
        user <- users.get(token.id).filterOrFail(_.lastRevoke <= token.issueTime)(ValidationError("Token revoked"))
      } yield user

      override def changeSecret(id: Long, secret: String): IO[DomainError, UserToken] = for {
        revokeTime <- clock.nanoTime
        _ <- repo.changeSecret(id, secret, revokeTime)
        token <- crypto.getSignedToken(id, revokeTime)
      } yield token

      override def revokeTokens(id: Long): IO[DomainError, Unit] = for {
        revokeTime <- clock.nanoTime
        _ <- repo.revokeTokens(id, revokeTime)
      } yield ()
    }
  }

  def login(email: String, secret: String): ZIO[AuthService, DomainError, UserToken] =
    ZIO.accessM(_.get.login(email, secret))

  def signIn(name: String, email: String, secret: String): ZIO[AuthService, DomainError, UserToken] =
    ZIO.accessM(_.get.signIn(name, email, secret))

  def validateUser(token: String): ZIO[AuthService, DomainError, User] =
    ZIO.accessM(_.get.validateUser(token))

  def changeSecret(id: Long, secret: String): ZIO[AuthService, DomainError, UserToken] =
    ZIO.accessM(_.get.changeSecret(id, secret))

  def revokeTokens(id: Long): ZIO[AuthService, DomainError, Unit] =
    ZIO.accessM(_.get.revokeTokens(id))
}
