package tevent.infrastructure.repository.tables

import slick.jdbc.{JdbcProfile, JdbcType}
import slick.lifted.ProvenShape
import tevent.domain.model.{EventParticipationType, EventSubscriber, OfflineParticipant, OnlineParticipant}

class EventParticipantsTable(val profile: JdbcProfile, val users: UsersTable, val events: EventsTable) {
  import profile.api._

  implicit val eventParticipationColumnType: JdbcType[EventParticipationType] = MappedColumnType.base[EventParticipationType, Int](
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

  case class EventParticipation(userId: Long, eventId: Long, participationType: EventParticipationType)

  class EventParticipantsTable(tag: Tag) extends Table[EventParticipation](tag, "ORG_PARTICIPANTS") {
    def userId: Rep[Long] = column("USER_ID")
    def eventId: Rep[Long] = column("EVENT_ID")
    def participationType: Rep[EventParticipationType] = column("TYPE")

    def user = foreignKey("USER_FK", userId, users.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def event = foreignKey("EVENT_FK", eventId, events.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    override def * : ProvenShape[EventParticipation] = (userId, eventId, participationType).mapTo[EventParticipation]
  }

  val All = TableQuery[EventParticipantsTable]
}
