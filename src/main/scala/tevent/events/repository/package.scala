package tevent.events

import tevent.events.repository.tables.{EventParticipantsTable, EventsTable}
import zio.Has

package object repository {
  type EventsT = Has[EventsTable]
  type EventParticipantsT = Has[EventParticipantsTable]

  type EventsRepository = Has[EventsRepository.Service]
  type EventParticipantsRepository = Has[EventParticipantsRepository.Service]
}
