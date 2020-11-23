package tevent.http.model.organization

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.domain.model.OrgParticipationType

case class OrgParticipationForm(participationType: OrgParticipationType)

object OrgParticipationForm {
  implicit val orgFormEncoder: Encoder[OrgParticipationForm] = deriveEncoder[OrgParticipationForm]
  implicit val orgFormDecoder: Decoder[OrgParticipationForm] = deriveDecoder[OrgParticipationForm]
}
