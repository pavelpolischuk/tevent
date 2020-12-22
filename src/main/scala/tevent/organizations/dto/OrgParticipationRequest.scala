package tevent.organizations.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.organizations.model.OrgParticipationType
import tevent.user.dto.UserData
import tevent.user.model.User

case class OrgParticipationRequest(user: UserData,
                                   participationType: OrgParticipationType,
                                   invitingUser: Option[UserData])

object OrgParticipationRequest {
  implicit val orgFormEncoder: Encoder[OrgParticipationRequest] = deriveEncoder[OrgParticipationRequest]
  implicit val orgFormDecoder: Decoder[OrgParticipationRequest] = deriveDecoder[OrgParticipationRequest]

  def mapperTo(request: (User, OrgParticipationType, User)): OrgParticipationRequest =
    OrgParticipationRequest(UserData.mapperTo(request._1), request._2,
      Option.when(request._1.id != request._3.id)(UserData.mapperTo(request._3)))
}
