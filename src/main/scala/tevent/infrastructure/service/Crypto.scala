package tevent.infrastructure.service

import tevent.domain.model.UserToken
import tevent.domain.{DomainError, ExecutionError, ValidationError}
import tevent.infrastructure.Configuration.{Config, HttpServerConfig}
import zio.{IO, Task, URLayer, ZIO}

object Crypto {

  trait Service {
    def dbSecret(userSecret: String, rounds: Int = 12): IO[ExecutionError, String]
    def verifySecret(userSecret: String, dbSecret: String): IO[ExecutionError, Boolean]
    def sign(message: String): IO[ExecutionError, String]
    def getSignedToken(userId: Long, issueTime: Long): IO[ExecutionError, UserToken]
    def validateSignedToken(token: String): IO[DomainError, UserToken]
  }

  class BcryptService(secret: String) extends Service {
    import com.github.t3hnar.bcrypt.BCryptStrOps
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec

    private val key = new SecretKeySpec(scala.io.Codec.toUTF8(secret), 0, 20,"HmacSHA1")

    override def dbSecret(userSecret: String, rounds: Int): IO[ExecutionError, String] =
      IO.fromTry(userSecret.bcryptSafeBounded(rounds)).mapError(ExecutionError)

    override def verifySecret(userSecret: String, dbSecret: String): IO[ExecutionError, Boolean] =
      IO.fromTry(userSecret.isBcryptedSafeBounded(dbSecret)).mapError(ExecutionError)

    override def sign(message: String): IO[ExecutionError, String] =
      Task {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(key)
        mac.doFinal(message.getBytes("utf-8")).map("%02X" format _).mkString
      }.mapError(ExecutionError)

    override def getSignedToken(userId: Long, issueTime: Long): IO[ExecutionError, UserToken] = for {
      joined <- UserToken.unsignedString(userId, issueTime)
      sign <- sign(joined)
    } yield UserToken(sign, userId, issueTime)

    override def validateSignedToken(token: String): IO[DomainError, UserToken] =
      UserToken.fromSignedString(token).tap(t => UserToken.unsignedString(t.id, t.issueTime).flatMap(sign)
        .filterOrFail[DomainError](t.sign.equals)(ValidationError("Invalid token")))
  }

  val bcrypt: URLayer[Config, Crypto] =
    ZIO.access[Config](_.get[HttpServerConfig].secret).map(new BcryptService(_)).toLayer


  def dbSecret(userSecret: String, rounds: Int = 12): ZIO[Crypto, ExecutionError, String] =
    ZIO.accessM(_.get.dbSecret(userSecret, rounds))

  def verifySecret(userSecret: String, dbSecret: String): ZIO[Crypto, ExecutionError, Boolean] =
    ZIO.accessM(_.get.verifySecret(userSecret, dbSecret))

  def getSignedToken(userId: Long, issueTime: Long): ZIO[Crypto, ExecutionError, UserToken] =
    ZIO.accessM(_.get.getSignedToken(userId, issueTime))

  def validateSignedToken(token: String): ZIO[Crypto, DomainError, UserToken] =
    ZIO.accessM(_.get.validateSignedToken(token))
}
