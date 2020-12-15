package tevent.user.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.user.model.User

case class UserId(id: Long, name: String)

object UserId {
  implicit val userEncoder: Encoder[UserId] = deriveEncoder[UserId]
  implicit val userDecoder: Decoder[UserId] = deriveDecoder[UserId]

  def mapperTo(user: User): UserId = UserId(user.id, user.name)
}
