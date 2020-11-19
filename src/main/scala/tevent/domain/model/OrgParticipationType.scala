package tevent.domain.model

import io.circe.{Decoder, Encoder}

sealed trait OrgParticipationType
case object OrgSubscriber extends OrgParticipationType
case object OrgMember extends OrgParticipationType
case object OrgManager extends OrgParticipationType
case object OrgOwner extends OrgParticipationType

object OrgParticipationType {
  implicit val orgParticipationEncoder: Encoder[OrgParticipationType] = Encoder[String].contramap {
    case OrgSubscriber => "sub"
    case OrgMember => "member"
    case OrgManager => "manager"
    case OrgOwner => "owner"
  }

  implicit val orgParticipationDecoder: Decoder[OrgParticipationType] = Decoder[String].emap {
    case "sub" => Right(OrgSubscriber)
    case "member" => Right(OrgMember)
    case "manager" => Right(OrgManager)
    case "owner" => Right(OrgOwner)
    case other => Left(s"Invalid OrgParticipationType: $other")
  }
}
