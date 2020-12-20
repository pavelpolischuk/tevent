package tevent.user.mock

import tevent.core.DomainError
import tevent.user.model.{User, UserToken}
import tevent.user.service.Auth
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object AuthMock extends Mock[Auth] {
  object Login extends Effect[(String, String), DomainError, UserToken]
  object SignIn extends Effect[(String, String, String), DomainError, UserToken]
  object Google extends Effect[String, DomainError, UserToken]
  object ValidateUser extends Effect[String, DomainError, User]
  object ChangeSecret extends Effect[(Long, String), DomainError, UserToken]
  object RevokeTokens extends Effect[Long, DomainError, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[Auth] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], Auth] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new Auth.Service {
        override def login(email: String, secret: String): IO[DomainError, UserToken] = proxy(Login, email, secret)
        override def signIn(name: String, email: String, secret: String): IO[DomainError, UserToken] = proxy(SignIn, name, email, secret)
        override def google(idToken: String): IO[DomainError, UserToken] = proxy(Google, idToken)
        override def validateUser(token: String): IO[DomainError, User] = proxy(ValidateUser, token)
        override def changeSecret(id: Long, secret: String): IO[DomainError, UserToken] = proxy(ChangeSecret, id, secret)
        override def revokeTokens(id: Long): IO[DomainError, Unit] = proxy(RevokeTokens, id)
      }
    }
  }
}
