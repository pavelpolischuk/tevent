package tevent.user.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class UserForm(name: String, email: String)

object UserForm {
  implicit val userFormEncoder: Encoder[UserForm] = deriveEncoder[UserForm]
  implicit val userFormDecoder: Decoder[UserForm] = deriveDecoder[UserForm]
}
