package tevent.infrastructure.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import tevent.domain.model.{Event, EventParticipation, EventParticipationType, User}

class EventParticipantsTable(val users: UsersTable, val events: EventsTable)(implicit val profile: JdbcProfile) {
  import profile.api._

  class EventParticipantsTable(tag: Tag) extends Table[EventParticipation](tag, "EVENT_PARTICIPANTS") {
    def userId: Rep[Long] = column("USER_ID")
    def eventId: Rep[Long] = column("EVENT_ID")
    def participationType: Rep[EventParticipationType] = column("TYPE")

    def user = foreignKey("EVENT_PART_USER_FK", userId, users.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def event = foreignKey("EVENT_PART_EVENT_FK", eventId, events.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
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

  def addUserTo(participation: EventParticipation): DBIO[Int] =
    All += participation

  def update(participation: EventParticipation): DBIO[Int] =
    All.filter(p => p.userId === participation.userId && p.eventId === participation.eventId)
      .map(_.participationType)
      .update(participation.participationType)

  def removeUserFrom(userId: Long, eventId: Long): DBIO[Int] =
    All.filter(p => p.userId === userId && p.eventId === eventId).delete
}
