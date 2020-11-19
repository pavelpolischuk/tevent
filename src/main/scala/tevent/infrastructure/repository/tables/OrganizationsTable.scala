package tevent.infrastructure.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import tevent.domain.model.Organization

class OrganizationsTable(val profile: JdbcProfile) {
  import profile.api._

  class Organizations(tag: Tag) extends Table[Organization](tag, "ORGANIZATIONS") {
    def id: Rep[Long] = column("ID", O.PrimaryKey, O.AutoInc)
    def name: Rep[String] = column("NAME")

    override def * : ProvenShape[Organization] = (id.?, name).mapTo[Organization]
  }

  val All = TableQuery[Organizations]

  def all: DBIO[Seq[Organization]] = All.result

  def add(organization: Organization): DBIO[Long] = (All returning All.map(_.id)) += organization

  def withId(id: Long): DBIO[Option[Organization]] = All.filter(_.id === id).result.headOption

  def update(organization: Organization): DBIO[Int] = {
    val q = for { c <- All if c.id === organization.id } yield c.name
    q.update(organization.name)
  }
}
