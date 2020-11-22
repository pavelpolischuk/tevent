package tevent.http.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class LoginData(message: String, token: String)

object LoginData {
  implicit val loginDataDecoder: Decoder[LoginData] = deriveDecoder[LoginData]
  implicit val loginDataEncoder: Encoder[LoginData] = deriveEncoder[LoginData]
}
