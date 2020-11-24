package tevent.http.model.event

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.domain.model.{Event, EventParticipationType}

case class EventParticipationData(event: Event, participation: EventParticipationType)

object EventParticipationData {
  implicit val eventParticipationEncoder: Encoder[EventParticipationData] = deriveEncoder[EventParticipationData]
  implicit val eventParticipationDecoder: Decoder[EventParticipationData] = deriveDecoder[EventParticipationData]

  def mapperTo(tuple: (Event, EventParticipationType)): EventParticipationData =
    EventParticipationData(tuple._1, tuple._2)
}
