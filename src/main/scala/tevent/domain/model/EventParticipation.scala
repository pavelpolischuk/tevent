package tevent.domain.model

case class EventParticipation(userId: Long,
                              eventId: Long,
                              participationType: EventParticipationType)
