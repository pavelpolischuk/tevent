package tevent.user.mock

import tevent.core.{DomainError, ExecutionError}
import tevent.user.model.UserToken
import tevent.user.service.Crypto
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object CryptoMock extends Mock[Crypto] {
  object DbSecret extends Effect[(String, Int), ExecutionError, String]
  object VerifySecret extends Effect[(String, String), ExecutionError, Boolean]
  object GetSignedToken extends Effect[(Long, Long), ExecutionError, UserToken]
  object ValidateSignedToken extends Effect[String, DomainError, UserToken]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[Crypto] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], Crypto] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      new Crypto.Service {
        override def dbSecret(userSecret: String, rounds: Int): IO[ExecutionError, String] = proxy(DbSecret, userSecret, rounds)
        override def verifySecret(userSecret: String, dbSecret: String): IO[ExecutionError, Boolean] = proxy(VerifySecret, userSecret, dbSecret)
        override def getSignedToken(userId: Long, issueTime: Long): IO[ExecutionError, UserToken] = proxy(GetSignedToken, userId, issueTime)
        override def validateSignedToken(token: String): IO[DomainError, UserToken] = proxy(ValidateSignedToken, token)
      }
    }
  }
}
