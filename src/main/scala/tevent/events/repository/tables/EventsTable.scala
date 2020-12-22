package tevent.events.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import slick.sql.SqlProfile.ColumnOption.Nullable
import tevent.events.model.{Event, EventFilter}
import tevent.organizations.model.PlainOrganization
import tevent.organizations.repository.tables.OrganizationsTable

import java.time.ZonedDateTime

class EventsTable(val organizations: OrganizationsTable)(implicit val profile: JdbcProfile) {
  import profile.api._

  class Events(tag: Tag) extends Table[Event](tag, "EVENTS") {
    def id: Rep[Long] = column("ID", O.PrimaryKey, O.AutoInc)
    def organizationId: Rep[Long] = column("ORGANIZATION_ID")
    def name: Rep[String] = column("NAME")
    def description: Rep[String] = column("DESCRIPTION")
    def datetime: Rep[ZonedDateTime] = column("DATETIME")
    def location: Rep[Option[String]] = column("LOCATION", Nullable)
    def capacity: Rep[Option[Int]] = column("CAPACITY", Nullable)
    def broadcastLink: Rep[Option[String]] = column("BROADCAST_LINK", Nullable)
    def tags: Rep[String] = column("TAGS")

    def organization = foreignKey("EVENT_ORGANIZATION_FK", organizationId, organizations.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    override def * : ProvenShape[Event] = (id, organizationId, name, description, datetime, location, capacity, broadcastLink, tags).<>(Event.mapperTo, Event.mapperFrom)
  }

  val All = TableQuery[Events]

  def all: DBIO[Seq[(Event, PlainOrganization)]] = (
    for {
      e <- All
      o <- e.organization
    } yield (e, o)).result

  def add(event: Event): DBIO[Long] =
    (All.map(e => (e.organizationId, e.name, e.description, e.datetime, e.location, e.capacity, e.broadcastLink, e.tags)) returning All.map(_.id)) +=
      (event.organizationId, event.name, event.description, event.datetime, event.location, event.capacity, event.videoBroadcastLink, Event.tagsToStr(event.tags))

  def withId(id: Long): DBIO[Option[(Event, PlainOrganization)]] = (
    for {
      e <- All if e.id === id
      o <- e.organization
    } yield (e, o)).result.headOption

  def where(eventFilter: EventFilter): DBIO[Seq[(Event, PlainOrganization)]] = {
    val prepared = All
      .filterOpt(eventFilter.organizationId)(_.organizationId === _)
      .filterOpt(eventFilter.fromDate)(_.datetime >= _)
      .filterOpt(eventFilter.toDate)(_.datetime <= _)
      .filterOpt(eventFilter.location)(_.location === _)

    val events = eventFilter.tags.foldLeft(prepared)((q, t) => q.withFilter(_.tags like s"%:$t:%"))
    (for {
      e <- events
      o <- e.organization
    } yield (e, o)).result
  }

  def update(event: Event): DBIO[Int] = {
    val q = for { c <- All if c.id === event.id } yield (c.name, c.description, c.datetime, c.location, c.capacity, c.broadcastLink, c.tags)
    q.update((event.name, event.description, event.datetime, event.location, event.capacity, event.videoBroadcastLink, Event.tagsToStr(event.tags)))
  }
}
