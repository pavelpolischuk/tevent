package tevent.http.model.organization

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.domain.model.{OrgParticipationType, User}
import tevent.http.model.user.UserId

case class OrgUserParticipationData(user: UserId, participation: OrgParticipationType)

object OrgUserParticipationData {
  implicit val orgParticipationEncoder: Encoder[OrgUserParticipationData] = deriveEncoder[OrgUserParticipationData]
  implicit val orgParticipationDecoder: Decoder[OrgUserParticipationData] = deriveDecoder[OrgUserParticipationData]

  def mapperTo(tuple: (User, OrgParticipationType)): OrgUserParticipationData =
    OrgUserParticipationData(UserId.mapperTo(tuple._1), tuple._2)
}
