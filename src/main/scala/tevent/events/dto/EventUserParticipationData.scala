package tevent.events.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.events.model.EventParticipationType
import tevent.user.dto.UserData
import tevent.user.model.User

case class EventUserParticipationData(user: UserData, participation: EventParticipationType)

object EventUserParticipationData {
  implicit val orgParticipationEncoder: Encoder[EventUserParticipationData] = deriveEncoder[EventUserParticipationData]
  implicit val orgParticipationDecoder: Decoder[EventUserParticipationData] = deriveDecoder[EventUserParticipationData]

  def mapperTo(tuple: (User, EventParticipationType)): EventUserParticipationData =
    EventUserParticipationData(UserData.mapperTo(tuple._1), tuple._2)
}
