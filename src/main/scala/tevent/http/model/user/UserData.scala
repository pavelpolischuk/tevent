package tevent.http.model.user

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.domain.model.User

case class UserData(name: String, email: String)

object UserData {
  implicit val userEncoder: Encoder[UserData] = deriveEncoder[UserData]
  implicit val userDecoder: Decoder[UserData] = deriveDecoder[UserData]

  def apply(user: User): UserData = UserData(user.name, user.email)
}
