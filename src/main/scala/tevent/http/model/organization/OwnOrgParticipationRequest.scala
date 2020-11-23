package tevent.http.model.organization

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.domain.model.{OrgParticipationType, Organization, User}
import tevent.http.model.user.UserId

case class OwnOrgParticipationRequest(organization: Organization,
                                      participationType: OrgParticipationType,
                                      invitingUser: Option[UserId])

object OwnOrgParticipationRequest {
  implicit val orgFormEncoder: Encoder[OwnOrgParticipationRequest] = deriveEncoder[OwnOrgParticipationRequest]
  implicit val orgFormDecoder: Decoder[OwnOrgParticipationRequest] = deriveDecoder[OwnOrgParticipationRequest]

  def mapperTo(request: (Organization, OrgParticipationType, Option[User])): OwnOrgParticipationRequest =
    OwnOrgParticipationRequest(request._1, request._2, request._3.map(UserId.mapperTo))
}
