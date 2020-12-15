package tevent.events.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.events.model.EventParticipationType
import tevent.user.dto.UserId
import tevent.user.model.User

case class EventUserParticipationData(user: UserId, participation: EventParticipationType)

object EventUserParticipationData {
  implicit val orgParticipationEncoder: Encoder[EventUserParticipationData] = deriveEncoder[EventUserParticipationData]
  implicit val orgParticipationDecoder: Decoder[EventUserParticipationData] = deriveDecoder[EventUserParticipationData]

  def mapperTo(tuple: (User, EventParticipationType)): EventUserParticipationData =
    EventUserParticipationData(UserId.mapperTo(tuple._1), tuple._2)

}
