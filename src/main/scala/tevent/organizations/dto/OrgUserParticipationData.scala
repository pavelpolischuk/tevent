package tevent.organizations.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.organizations.model.OrgParticipationType
import tevent.user.dto.UserData
import tevent.user.model.User

case class OrgUserParticipationData(user: UserData, participation: OrgParticipationType)

object OrgUserParticipationData {
  implicit val orgParticipationEncoder: Encoder[OrgUserParticipationData] = deriveEncoder[OrgUserParticipationData]
  implicit val orgParticipationDecoder: Decoder[OrgUserParticipationData] = deriveDecoder[OrgUserParticipationData]

  def mapperTo(tuple: (User, OrgParticipationType)): OrgUserParticipationData =
    OrgUserParticipationData(UserData.mapperTo(tuple._1), tuple._2)
}
