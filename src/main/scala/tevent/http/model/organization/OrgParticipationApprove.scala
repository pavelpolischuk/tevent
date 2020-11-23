package tevent.http.model.organization

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class OrgParticipationApprove(userId: Long)

object OrgParticipationApprove {
  implicit val orgFormEncoder: Encoder[OrgParticipationApprove] = deriveEncoder[OrgParticipationApprove]
  implicit val orgFormDecoder: Decoder[OrgParticipationApprove] = deriveDecoder[OrgParticipationApprove]
}
