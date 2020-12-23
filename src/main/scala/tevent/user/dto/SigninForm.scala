package tevent.user.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class SigninForm(name: String, email: String, secret: String)

object SigninForm {
  implicit val signinFormDecoder: Decoder[SigninForm] = deriveDecoder[SigninForm]
  implicit val signinFormEncoder: Encoder[SigninForm] = deriveEncoder[SigninForm]
}
