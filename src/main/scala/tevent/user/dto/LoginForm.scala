package tevent.user.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class LoginForm(name: Option[String], email: String, secret: String)

object LoginForm {
  implicit val loginFormDecoder: Decoder[LoginForm] = deriveDecoder[LoginForm]
  implicit val loginFormEncoder: Encoder[LoginForm] = deriveEncoder[LoginForm]
}
