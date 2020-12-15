package tevent.events.model

case class EventParticipation(userId: Long,
                              eventId: Long,
                              participationType: EventParticipationType)
