package tevent.http.model.user

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import tevent.domain.model.User

case class UserId(id: Long, name: String)

object UserId {
  implicit val userEncoder: Encoder[UserId] = deriveEncoder[UserId]
  implicit val userDecoder: Decoder[UserId] = deriveDecoder[UserId]

  def mapperTo(user: User): UserId = UserId(user.id, user.name)
}
