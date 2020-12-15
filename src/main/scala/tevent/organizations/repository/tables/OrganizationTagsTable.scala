package tevent.organizations.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

class OrganizationTagsTable(val organizations: OrganizationsTable)(implicit val profile: JdbcProfile) {
  import profile.api._

  case class OrganizationTag(organizationId: Long, tag: String)

  object OrganizationTag {
    def mapperTo(tuple: (Long, String)): OrganizationTag =
      OrganizationTag(tuple._1, tuple._2)
  }

  class OrganizationTags(tag: Tag) extends Table[OrganizationTag](tag, "ORGANIZATION_TAGS") {
    def organizationId: Rep[Long] = column("ORGANIZATION_ID")
    def orgTag: Rep[String] = column("TAG")

    def organization = foreignKey("ORG_TAG_ORGANIZATION_FK", organizationId, organizations.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    override def * : ProvenShape[OrganizationTag] = (organizationId, orgTag).<>(OrganizationTag.mapperTo, OrganizationTag.unapply)
  }

  val All = TableQuery[OrganizationTags]

  def findOrganizations(tags: Iterable[String]): DBIO[Seq[Long]] =
    All.filter(_.orgTag.inSet(tags)).groupBy(_.organizationId)
      .map(t => (t._1, t._2.length))
      .sortBy(_._2.desc)
      .map(_._1).result

  def add(id: Long, tags: List[String]): DBIO[Option[Int]] =
    All.map(o => (o.organizationId, o.orgTag)) ++= tags.map((id, _))

  def withId(id: Long): DBIO[Seq[String]] =
    All.filter(_.organizationId === id).map(_.orgTag).result

  def update(id: Long, tags: List[String]): DBIO[Option[Int]] =
    All.filter(_.organizationId === id).delete.andThen(
      All.map(o => (o.organizationId, o.orgTag)) ++= tags.map((id, _))
    )
}
