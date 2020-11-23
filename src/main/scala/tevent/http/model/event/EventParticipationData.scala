package tevent.http.model.event

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import tevent.domain.model.{Event, EventParticipationType}

case class EventParticipationData(event: Event, participation: EventParticipationType)

object EventParticipationData {
  implicit val eventParticipationEncoder: Encoder[EventParticipationData] = deriveEncoder[EventParticipationData]
  implicit val eventParticipationDecoder: Decoder[EventParticipationData] = deriveDecoder[EventParticipationData]

  implicit val eventParticipationTupleEncoder: Encoder[(Event, EventParticipationType)] =
    eventParticipationEncoder.contramap(p => EventParticipationData(p._1, p._2))
  implicit val eventParticipationTupleDecoder: Decoder[(Event, EventParticipationType)] =
    eventParticipationDecoder.map(p => (p.event, p.participation))
}
