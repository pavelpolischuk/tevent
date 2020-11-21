package tevent.http.model

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import tevent.domain.model.{Event, EventParticipationType}

case class EventParticipation(event: Event, participation: EventParticipationType)

object EventParticipation {

  implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]

  implicit val eventParticipationEncoder: Encoder[(Event, EventParticipationType)] =
    deriveEncoder[EventParticipation].contramap(p => EventParticipation(p._1, p._2))
}
