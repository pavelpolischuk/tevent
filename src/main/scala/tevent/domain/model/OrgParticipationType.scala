package tevent.domain.model

import io.circe.{Decoder, Encoder}

sealed trait OrgParticipationType {
  val value: Int

  def >=(other: OrgParticipationType): Boolean = {
    value >= other.value
  }
}

case object OrgSubscriber extends OrgParticipationType {
  override val value: Int = 100
}
case object OrgMember extends OrgParticipationType {
  override val value: Int = 200
}
case object OrgManager extends OrgParticipationType {
  override val value: Int = 300
}
case object OrgOwner extends OrgParticipationType {
  override val value: Int = 400
}


object OrgParticipationType {
  implicit val orgParticipationTypeOrdering: Ordering[OrgParticipationType] =
    (o1: OrgParticipationType, o2: OrgParticipationType) => o1.value.compareTo(o2.value)

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
