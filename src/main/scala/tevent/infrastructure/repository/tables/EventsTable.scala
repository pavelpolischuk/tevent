package tevent.infrastructure.repository.tables

import java.time.ZonedDateTime
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import slick.sql.SqlProfile.ColumnOption.Nullable
import tevent.domain.model.{Event, EventFilter}

class EventsTable(val organizations: OrganizationsTable)(implicit val profile: JdbcProfile) {
  import profile.api._

  class Events(tag: Tag) extends Table[Event](tag, "EVENTS") {
    def id: Rep[Long] = column("ID", O.PrimaryKey, O.AutoInc)
    def organizationId: Rep[Long] = column("ORGANIZATION_ID")
    def name: Rep[String] = column("NAME")
    def datetime: Rep[ZonedDateTime] = column("DATETIME")
    def location: Rep[Option[String]] = column("LOCATION", Nullable)
    def capacity: Rep[Option[Int]] = column("CAPACITY", Nullable)
    def broadcastLink: Rep[Option[String]] = column("BROADCAST_LINK", Nullable)

    def organization = foreignKey("ORGANIZATION_FK", organizationId, organizations.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    override def * : ProvenShape[Event] = (id, organizationId, name, datetime, location, capacity, broadcastLink).<>(Event.mapperTo, Event.unapply)
  }

  val All = TableQuery[Events]

  def all: DBIO[Seq[Event]] = All.result

  def add(event: Event): DBIO[Long] =
    (All.map(e => (e.organizationId, e.name, e.datetime, e.location, e.capacity, e.broadcastLink)) returning All.map(_.id)) +=
      (event.organizationId, event.name, event.datetime, event.location, event.capacity, event.videoBroadcastLink)

  def withId(id: Long): DBIO[Option[Event]] = All.filter(_.id === id).result.headOption

  def where(eventFilter: EventFilter): DBIO[Seq[Event]] = All
      .filterOpt(eventFilter.organizationId)(_.organizationId === _)
      .filterOpt(eventFilter.fromDate)(_.datetime >= _)
      .filterOpt(eventFilter.toDate)(_.datetime <= _)
      .filterOpt(eventFilter.location)(_.location === _)
      .result

  def update(event: Event): DBIO[Int] = {
    val q = for { c <- All if c.id === event.id } yield (c.name, c.datetime, c.location, c.capacity, c.broadcastLink)
    q.update((event.name, event.datetime, event.location, event.capacity, event.videoBroadcastLink))
  }
}
