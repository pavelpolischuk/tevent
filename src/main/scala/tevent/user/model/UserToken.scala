package tevent.user.model

import tevent.core.{TokenFormatInvalid, ValidationError}
import zio.{IO, UIO, ZIO}

case class UserToken(sign: String, id: Long, issueTime: Long) {
  def signedString: String = s"$sign.$id.$issueTime"
}

object UserToken {
  def unsignedString(id: Long, issueTime: Long): UIO[String] =
    ZIO.succeed(s"$id.$issueTime")

  def fromSignedString(value: String): IO[ValidationError, UserToken] =
    ZIO.succeed(value.split('.'))
      .filterOrFail(_.length == 3)(TokenFormatInvalid)
      .map(ar => (ar(0), ar(1).toLongOption, ar(2).toLongOption))
      .flatMap {
        case (sign, Some(id), Some(issueTime)) => ZIO.succeed(UserToken(sign, id, issueTime))
        case _ => ZIO.fail(TokenFormatInvalid)
      }
}
