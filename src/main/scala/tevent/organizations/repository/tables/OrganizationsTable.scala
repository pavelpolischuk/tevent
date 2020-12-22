package tevent.organizations.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import tevent.organizations.model.{Organization, PlainOrganization}

class OrganizationsTable(implicit val profile: JdbcProfile) {
  import profile.api._

  class Organizations(tag: Tag) extends Table[PlainOrganization](tag, "ORGANIZATIONS") {
    def id: Rep[Long] = column("ID", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column("NAME")
    def nick: Rep[String] = column("NICK")
    def description: Rep[String] = column("DESCRIPTION")

    override def * : ProvenShape[PlainOrganization] = (id, name, nick, description).<>(PlainOrganization.mapperTo, PlainOrganization.unapply)
  }

  val All = TableQuery[Organizations]

  def all: DBIO[Seq[PlainOrganization]] = All.result

  def add(organization: Organization): DBIO[Long] =
    (All.map(o => (o.name, o.nick, o.description)) returning All.map(_.id)) += (organization.name, organization.nick, organization.description)

  def withId(id: Long): DBIO[Option[PlainOrganization]] =
    All.filter(_.id === id).result.headOption

  def update(organization: Organization): DBIO[Int] = {
    val q = for { c <- All if c.id === organization.id } yield (c.name, c.nick, c.description)
    q.update((organization.name, organization.nick, organization.description))
  }
}
