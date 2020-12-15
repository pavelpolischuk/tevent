package tevent.user.service

import tevent.core.{DomainError, ValidationError}
import tevent.user.model.{User, UserToken}
import tevent.user.repository.UsersRepository
import zio.clock.Clock
import zio.{IO, URLayer, ZIO, ZLayer}

object Auth {
  trait Service {
    def login(email: String, secret: String): IO[DomainError, UserToken]
    def signIn(name: String, email: String, secret: String): IO[DomainError, UserToken]
    def validateUser(token: String): IO[DomainError, User]
    def changeSecret(id: Long, secret: String): IO[DomainError, UserToken]
    def revokeTokens(id: Long): IO[DomainError, Unit]
  }

  val live: URLayer[Clock with Crypto with Users with UsersRepository, Auth] =
    ZLayer.fromServices[Clock.Service, Crypto.Service, Users.Service, UsersRepository.Service, Service] {
      (clock, crypto, users, repo) => new Service {

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

  def login(email: String, secret: String): ZIO[Auth, DomainError, UserToken] =
    ZIO.accessM(_.get.login(email, secret))

  def signIn(name: String, email: String, secret: String): ZIO[Auth, DomainError, UserToken] =
    ZIO.accessM(_.get.signIn(name, email, secret))

  def validateUser(token: String): ZIO[Auth, DomainError, User] =
    ZIO.accessM(_.get.validateUser(token))

  def changeSecret(id: Long, secret: String): ZIO[Auth, DomainError, UserToken] =
    ZIO.accessM(_.get.changeSecret(id, secret))

  def revokeTokens(id: Long): ZIO[Auth, DomainError, Unit] =
    ZIO.accessM(_.get.revokeTokens(id))
}
