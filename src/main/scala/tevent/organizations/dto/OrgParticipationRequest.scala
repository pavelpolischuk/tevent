package tevent.organizations.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.organizations.model.OrgParticipationType
import tevent.user.dto.UserId
import tevent.user.model.User

case class OrgParticipationRequest(user: UserId,
                                   participationType: OrgParticipationType,
                                   invitingUser: Option[UserId])

object OrgParticipationRequest {
  implicit val orgFormEncoder: Encoder[OrgParticipationRequest] = deriveEncoder[OrgParticipationRequest]
  implicit val orgFormDecoder: Decoder[OrgParticipationRequest] = deriveDecoder[OrgParticipationRequest]

  def mapperTo(request: (User, OrgParticipationType, User)): OrgParticipationRequest =
    OrgParticipationRequest(UserId.mapperTo(request._1), request._2,
      Option.when(request._1.id != request._3.id)(UserId.mapperTo(request._3)))
}
