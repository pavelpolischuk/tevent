package tevent.http.model.organization

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class OrganizationForm(name: String)

object OrganizationForm {
  implicit val orgFormEncoder: Encoder[OrganizationForm] = deriveEncoder[OrganizationForm]
  implicit val orgFormDecoder: Decoder[OrganizationForm] = deriveDecoder[OrganizationForm]
}
