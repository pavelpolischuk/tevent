package tevent.http.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import tevent.domain.model.{Event, EventParticipationType}

case class EventParticipation(event: Event, participation: EventParticipationType)

object EventParticipation {
  implicit val eventParticipationEncoder: Encoder[EventParticipation] = deriveEncoder[EventParticipation]
  implicit val eventParticipationDecoder: Decoder[EventParticipation] = deriveDecoder[EventParticipation]

  implicit val eventParticipationTupleEncoder: Encoder[(Event, EventParticipationType)] =
    eventParticipationEncoder.contramap(p => EventParticipation(p._1, p._2))
  implicit val eventParticipationTupleDecoder: Decoder[(Event, EventParticipationType)] =
    eventParticipationDecoder.map(p => (p.event, p.participation))
}
