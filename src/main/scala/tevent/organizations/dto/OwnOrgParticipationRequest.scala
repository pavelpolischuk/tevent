package tevent.organizations.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.organizations.model.{OrgParticipationType, Organization}
import tevent.user.dto.UserData
import tevent.user.model.User

case class OwnOrgParticipationRequest(organization: Organization,
                                      participationType: OrgParticipationType,
                                      invitingUser: Option[UserData])

object OwnOrgParticipationRequest {
  implicit val orgFormEncoder: Encoder[OwnOrgParticipationRequest] = deriveEncoder[OwnOrgParticipationRequest]
  implicit val orgFormDecoder: Decoder[OwnOrgParticipationRequest] = deriveDecoder[OwnOrgParticipationRequest]

  def mapperTo(request: (Organization, OrgParticipationType, Option[User])): OwnOrgParticipationRequest =
    OwnOrgParticipationRequest(request._1, request._2, request._3.map(UserData.mapperTo))
}
