package tevent.events.repository

import slick.jdbc.{JdbcProfile, JdbcType}
import tevent.events.model.{EventParticipationType, EventSubscriber, OfflineParticipant, OnlineParticipant}

package object tables {
  private[repository] implicit def eventParticipationColumnType(implicit profile: JdbcProfile): JdbcType[EventParticipationType] = {
    import profile.api._

    MappedColumnType.base[EventParticipationType, Int](
      {
        case EventSubscriber => 0
        case OnlineParticipant => 1
        case OfflineParticipant => 2
      },
      {
        case 0 => EventSubscriber
        case 1 => OnlineParticipant
        case 2 => OfflineParticipant
      }
    )
  }
}
