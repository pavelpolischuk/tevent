package tevent.http.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class OrganizationForm(name: String)

object OrganizationForm {
  implicit val orgFormDecoder: Decoder[OrganizationForm] = deriveDecoder[OrganizationForm]
}
