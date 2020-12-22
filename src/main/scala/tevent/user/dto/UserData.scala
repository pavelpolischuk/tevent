package tevent.user.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.user.model.User

case class UserData(id: Long, name: String, email: String)

object UserData {
  implicit val userEncoder: Encoder[UserData] = deriveEncoder[UserData]
  implicit val userDecoder: Decoder[UserData] = deriveDecoder[UserData]

  def mapperTo(user: User): UserData = UserData(user.id, user.name, user.email)
}
