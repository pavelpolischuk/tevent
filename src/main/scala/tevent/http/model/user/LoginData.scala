package tevent.http.model.user

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class LoginData(message: String, token: String)

object LoginData {
  implicit val loginDataDecoder: Decoder[LoginData] = deriveDecoder[LoginData]
  implicit val loginDataEncoder: Encoder[LoginData] = deriveEncoder[LoginData]
}
