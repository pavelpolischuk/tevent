package tevent.infrastructure.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import tevent.domain.model._

class OrgParticipantsTable(val users: UsersTable, val organizations: OrganizationsTable)(implicit val profile: JdbcProfile) {
  import profile.api._

  class OrgParticipantsTable(tag: Tag) extends Table[OrgParticipation](tag, "ORG_PARTICIPANTS") {
    def userId: Rep[Long] = column("USER_ID")
    def organizationId: Rep[Long] = column("ORGANIZATION_ID")
    def participationType: Rep[OrgParticipationType] = column("TYPE")

    def user = foreignKey("ORG_PART_USER_FK", userId, users.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def organization = foreignKey("ORG_PART_ORGANIZATION_FK", organizationId, organizations.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def pk = primaryKey("ORG_PARTICIPANTS_PK", (userId, organizationId))

    override def * : ProvenShape[OrgParticipation] = (userId, organizationId, participationType).mapTo[OrgParticipation]
  }

  val All = TableQuery[OrgParticipantsTable]

  def forUser(userId: Long): DBIO[Seq[(Organization, OrgParticipationType)]] = (for {
      part <- All if part.userId === userId
      org <- part.organization
    } yield (org, part.participationType) ).result

  def getUsersFrom(organizationId: Long): DBIO[Seq[(User, OrgParticipationType)]] = (for {
    part <- All if part.organizationId === organizationId
    user <- part.user
  } yield (user, part.participationType) ).result

  def checkUserIn(userId: Long, organizationId: Long): DBIO[Option[OrgParticipationType]] =
    All.filter(p => p.userId === userId && p.organizationId === organizationId)
      .map(_.participationType).result.headOption

  def add(participation: OrgParticipation): DBIO[Int] =
    All += participation

  def update(participation: OrgParticipation): DBIO[Int] =
    All.filter(p => p.userId === participation.userId && p.organizationId === participation.organizationId)
      .map(_.participationType)
      .update(participation.participationType)

  def removeUserFrom(userId: Long, organizationId: Long): DBIO[Int] =
    All.filter(p => p.userId === userId && p.organizationId === organizationId).delete
}
