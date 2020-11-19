package tevent.domain.model

import io.circe.{Decoder, Encoder}

sealed trait EventParticipationType
case object EventSubscriber extends EventParticipationType
case object OnlineParticipant extends EventParticipationType
case object OfflineParticipant extends EventParticipationType

object EventParticipationType {
  implicit val eventParticipationEncoder: Encoder[EventParticipationType] = Encoder[String].contramap {
    case EventSubscriber => "sub"
    case OnlineParticipant => "online"
    case OfflineParticipant => "offline"
  }

  implicit val eventParticipationDecoder: Decoder[EventParticipationType] = Decoder[String].emap {
    case "sub" => Right(EventSubscriber)
    case "online" => Right(OnlineParticipant)
    case "offline" => Right(OfflineParticipant)
    case other => Left(s"Invalid EventParticipationType: $other")
  }
}
