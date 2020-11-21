package tevent.http.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class UserData(name: String, email: String)

object UserData {
  implicit val userEncoder: Encoder[UserData] = deriveEncoder[UserData]
  implicit val userDecoder: Decoder[UserData] = deriveDecoder[UserData]
}
