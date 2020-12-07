package tevent.infrastructure.repository.tables

import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape
import tevent.domain.model._

class OrgParticipationRequestsTable(val users: UsersTable, val organizations: OrganizationsTable)(implicit val profile: JdbcProfile) {
  import profile.api._

  class OrgParticipationRequestsTable(tag: Tag) extends Table[OrgParticipationRequest](tag, "ORG_PARTICIPATION_REQUESTS") {
    def userId: Rep[Long] = column("USER_ID")
    def organizationId: Rep[Long] = column("ORGANIZATION_ID")
    def participationType: Rep[OrgParticipationType] = column("TYPE")
    def fromUserId: Rep[Long] = column("FROM_USER_ID")

    def user = foreignKey("ORG_PART_REQUEST_USER_FK", userId, users.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def organization = foreignKey("ORG_PART_REQUEST_ORGANIZATION_FK", organizationId, organizations.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def fromUser = foreignKey("ORG_PART_REQUEST_FROM_USER_FK", fromUserId, users.All)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def pk = index("ORG_PARTICIPATION_REQUESTS_PK", (userId, organizationId))

    override def * : ProvenShape[OrgParticipationRequest] = (userId, organizationId, participationType, fromUserId).<>(OrgParticipationRequest.mapperTo, OrgParticipationRequest.unapply)
  }

  val All = TableQuery[OrgParticipationRequestsTable]

  def forUser(userId: Long): DBIO[Seq[(Organization, OrgParticipationType, User)]] = (for {
      part <- All if part.userId === userId
      org <- part.organization
      fromUser <- part.fromUser
    } yield (org, part.participationType, fromUser) ).result

  def forOrganization(organizationId: Long): DBIO[Seq[(User, OrgParticipationType, User)]] = (for {
    part <- All if part.organizationId === organizationId
    user <- part.user
    fromUser <- part.fromUser
  } yield (user, part.participationType, fromUser) ).result

  def get(userId: Long, organizationId: Long): DBIO[Option[OrgParticipationRequest]] =
    All.filter(p => p.userId === userId && p.organizationId === organizationId).result.headOption

  def add(request: OrgParticipationRequest): DBIO[Int] =
    All += request

  def update(request: OrgParticipationRequest): DBIO[Int] =
    All.filter(p => p.userId === request.userId && p.organizationId === request.organizationId)
      .map(p => (p.participationType, p.fromUserId))
      .update((request.participationType, request.fromUserId))

  def remove(userId: Long, organizationId: Long): DBIO[Int] =
    All.filter(p => p.userId === userId && p.organizationId === organizationId).delete
}
