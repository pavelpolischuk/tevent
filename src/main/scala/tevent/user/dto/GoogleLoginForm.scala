package tevent.user.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class GoogleLoginForm(idtoken: String)

object GoogleLoginForm {
  implicit val googleLoginDecoder: Decoder[GoogleLoginForm] = deriveDecoder[GoogleLoginForm]
  implicit val googleLoginEncoder: Encoder[GoogleLoginForm] = deriveEncoder[GoogleLoginForm]
}
