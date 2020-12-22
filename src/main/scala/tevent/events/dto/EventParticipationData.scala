package tevent.events.dto

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.events.model.EventParticipationType

case class EventParticipationData(event: EventData, participation: EventParticipationType)

object EventParticipationData {
  implicit val eventParticipationEncoder: Encoder[EventParticipationData] = deriveEncoder[EventParticipationData]
  implicit val eventParticipationDecoder: Decoder[EventParticipationData] = deriveDecoder[EventParticipationData]

  def mapperTo(tuple: (EventData, EventParticipationType)): EventParticipationData =
    EventParticipationData(tuple._1, tuple._2)
}
