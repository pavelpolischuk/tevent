package tevent.http.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import tevent.domain.model.{OrgParticipationType, Organization}

case class OrgParticipation(org: Organization, participation: OrgParticipationType)

object OrgParticipation {
  implicit val orgParticipationEncoder: Encoder[OrgParticipation] = deriveEncoder[OrgParticipation]
  implicit val orgParticipationDecoder: Decoder[OrgParticipation] = deriveDecoder[OrgParticipation]

  implicit val orgParticipationTupleEncoder: Encoder[(Organization, OrgParticipationType)] =
    orgParticipationEncoder.contramap(p => OrgParticipation(p._1, p._2))
  implicit val orgParticipationTupleDecoder: Decoder[(Organization, OrgParticipationType)] =
    orgParticipationDecoder.map(p => (p.org, p.participation))
}
