package tevent.user.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class SecretForm(secret: String)

object SecretForm {
  implicit val secretFormDecoder: Decoder[SecretForm] = deriveDecoder[SecretForm]
  implicit val secretFormEncoder: Encoder[SecretForm] = deriveEncoder[SecretForm]
}
