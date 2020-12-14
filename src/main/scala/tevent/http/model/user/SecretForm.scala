package tevent.http.model.user

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class SecretForm(secret: String)

object SecretForm {
  implicit val secretFormDecoder: Decoder[SecretForm] = deriveDecoder[SecretForm]
  implicit val secretFormEncoder: Encoder[SecretForm] = deriveEncoder[SecretForm]
}
