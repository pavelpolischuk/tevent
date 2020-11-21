package tevent.http.model

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import tevent.domain.model.{OrgParticipationType, Organization}

case class OrgParticipation(org: Organization, participation: OrgParticipationType)

object OrgParticipation {

  implicit val organizationEncoder: Encoder[Organization] = deriveEncoder[Organization]

  implicit val orgParticipationEncoder: Encoder[(Organization, OrgParticipationType)] =
    deriveEncoder[OrgParticipation].contramap(p => OrgParticipation(p._1, p._2))
}
