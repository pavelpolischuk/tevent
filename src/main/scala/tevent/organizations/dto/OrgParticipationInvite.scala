package tevent.organizations.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.organizations.model.OrgParticipationType

case class OrgParticipationInvite(userId: Long, participationType: OrgParticipationType)

object OrgParticipationInvite {
  implicit val orgFormEncoder: Encoder[OrgParticipationInvite] = deriveEncoder[OrgParticipationInvite]
  implicit val orgFormDecoder: Decoder[OrgParticipationInvite] = deriveDecoder[OrgParticipationInvite]
}
