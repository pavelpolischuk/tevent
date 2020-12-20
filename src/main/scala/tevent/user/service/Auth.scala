package tevent.user.service

import tevent.core.{DomainError, ExecutionError, ValidationError}
import tevent.user.model.{Email, GoogleId, User, UserToken}
import tevent.user.repository.UsersRepository
import zio.clock.Clock
import zio.{IO, URLayer, ZIO, ZLayer}

object Auth {
  trait Service {
    def login(email: String, secret: String): IO[DomainError, UserToken]
    def signIn(name: String, email: String, secret: String): IO[DomainError, UserToken]
    def google(idToken: String): IO[DomainError, UserToken]
    def validateUser(token: String): IO[DomainError, User]
    def changeSecret(id: Long, secret: String): IO[DomainError, UserToken]
    def revokeTokens(id: Long): IO[DomainError, Unit]
  }

  val live: URLayer[Clock with Crypto with GoogleAuth with Users with UsersRepository, Auth] =
    ZLayer.fromServices[Clock.Service, Crypto.Service, GoogleAuth.Service, Users.Service, UsersRepository.Service, Service] {
      (clock, crypto, goo, users, repo) => new Service {

      override def login(email: String, secret: String): IO[DomainError, UserToken] = for {
        user <- users.find(Email(email))
        _ <- user.secretHash.fold[IO[ExecutionError, Boolean]](IO.succeed(false))(h => crypto.verifySecret(secret, h))
          .filterOrFail[DomainError](a => a)(ValidationError("Bad secret"))
        issueTime <- clock.nanoTime
        token <- crypto.getSignedToken(user.id, issueTime)
      } yield token

      override def signIn(name: String, email: String, secret: String): IO[DomainError, UserToken] = for {
        secret <- crypto.dbSecret(secret)
        user <- users.create(name, email, secret)
        issueTime <- clock.nanoTime
        token <- crypto.getSignedToken(user.id, issueTime)
      } yield token

      override def google(idToken: String): IO[DomainError, UserToken] = for {
        token <- goo.getInfo(idToken)
        userId <- repo.find(GoogleId(token.userId)).flatMap{
          case Some(u) => IO.succeed(u.id)
          case None => repo.add(User(token))
        }

        issueTime <- clock.nanoTime
        token <- crypto.getSignedToken(userId, issueTime)
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

  def google(idToken: String): ZIO[Auth, DomainError, UserToken] =
    ZIO.accessM(_.get.google(idToken))

  def validateUser(token: String): ZIO[Auth, DomainError, User] =
    ZIO.accessM(_.get.validateUser(token))

  def changeSecret(id: Long, secret: String): ZIO[Auth, DomainError, UserToken] =
    ZIO.accessM(_.get.changeSecret(id, secret))

  def revokeTokens(id: Long): ZIO[Auth, DomainError, Unit] =
    ZIO.accessM(_.get.revokeTokens(id))
}
