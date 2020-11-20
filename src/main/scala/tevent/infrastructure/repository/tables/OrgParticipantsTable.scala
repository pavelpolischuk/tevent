package tevent.infrastructure.repository.tables

import slick.jdbc.{JdbcProfile, JdbcType}
import slick.lifted.ProvenShape
import tevent.domain.model._

class OrgParticipantsTable(val profile: JdbcProfile,
                           val users: UsersTable,
                           val organizations: OrganizationsTable) {
  import profile.api._

  implicit val orgParticipationColumnType: JdbcType[OrgParticipationType] = MappedColumnType.base[OrgParticipationType, Int](
    {
      case OrgSubscriber => 0
      case OrgMember => 1
      case OrgManager => 2
      case OrgOwner => 3
    },
    {
      case 0 => OrgSubscriber
      case 1 => OrgMember
      case 2 => OrgManager
      case 3 => OrgOwner
    }
  )

  case class OrgParticipation(userId: Long, organizationId: Long, participationType: OrgParticipationType)

  class OrgParticipantsTable(tag: Tag) extends Table[OrgParticipation](tag, "ORG_PARTICIPANTS") {
    def userId: Rep[Long] = column("USER_ID")
    def organizationId: Rep[Long] = column("ORGANIZATION_ID")
    def participationType: Rep[OrgParticipationType] = column("TYPE")

    def user = foreignKey("USER_FK", userId, users.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def organization = foreignKey("ORGANIZATION_FK", organizationId, organizations.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    override def * : ProvenShape[OrgParticipation] = (userId, organizationId, participationType).mapTo[OrgParticipation]
  }

  val All = TableQuery[OrgParticipantsTable]
}