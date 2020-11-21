package tevent.infrastructure.repository.tables

import slick.jdbc.{JdbcProfile, JdbcType}
import slick.lifted.ProvenShape
import tevent.domain.model.{Event, EventParticipationType, EventSubscriber, OfflineParticipant, OnlineParticipant, User}

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

  class EventParticipantsTable(tag: Tag) extends Table[EventParticipation](tag, "EVENT_PARTICIPANTS") {
    def userId: Rep[Long] = column("USER_ID")
    def eventId: Rep[Long] = column("EVENT_ID")
    def participationType: Rep[EventParticipationType] = column("TYPE")

    def user = foreignKey("USER_FK", userId, users.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def event = foreignKey("EVENT_FK", eventId, events.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def pk = primaryKey("EVENT_PARTICIPANTS_PK", (userId, eventId))

    override def * : ProvenShape[EventParticipation] = (userId, eventId, participationType).mapTo[EventParticipation]
  }

  val All = TableQuery[EventParticipantsTable]

  def forUser(userId: Long): DBIO[Seq[(Event, EventParticipationType)]] = (for {
    part <- All if part.userId === userId
    event <- part.event
  } yield (event, part.participationType) ).result

  def getUsersBy(eventId: Long): DBIO[Seq[(User, EventParticipationType)]] = (for {
    part <- All if part.eventId === eventId
    user <- part.user
  } yield (user, part.participationType) ).result

  def checkUserIn(userId: Long, eventId: Long): DBIO[Option[EventParticipationType]] =
    All.filter(p => p.userId === userId && p.eventId === eventId).map(_.participationType).result.headOption

  def addUserTo(userId: Long, eventId: Long, role: EventParticipationType): DBIO[Int] =
    All += EventParticipation(userId, eventId, role)

  def updateUserTo(userId: Long, eventId: Long, role: EventParticipationType): DBIO[Int] =
    All.filter(p => p.userId === userId && p.eventId === eventId).map(_.participationType).update(role)

  def removeUserFrom(userId: Long, eventId: Long): DBIO[Int] =
    All.filter(p => p.userId === userId && p.eventId === eventId).delete
}
