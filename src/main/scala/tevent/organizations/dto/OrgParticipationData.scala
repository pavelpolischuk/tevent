package tevent.organizations.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.organizations.model.{OrgParticipationType, Organization}

case class OrgParticipationData(organization: Organization, participation: OrgParticipationType)

object OrgParticipationData {
  implicit val orgParticipationEncoder: Encoder[OrgParticipationData] = deriveEncoder[OrgParticipationData]
  implicit val orgParticipationDecoder: Decoder[OrgParticipationData] = deriveDecoder[OrgParticipationData]

  def mapperTo(tuple: (Organization, OrgParticipationType)): OrgParticipationData =
    OrgParticipationData(tuple._1, tuple._2)
}
