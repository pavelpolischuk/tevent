package tevent.http.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class OrganizationForm(name: String)

object OrganizationForm {
  implicit val orgFormEncoder: Encoder[OrganizationForm] = deriveEncoder[OrganizationForm]
  implicit val orgFormDecoder: Decoder[OrganizationForm] = deriveDecoder[OrganizationForm]
}
