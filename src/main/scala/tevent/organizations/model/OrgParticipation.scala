package tevent.organizations.model

import tevent.core.EntityType
import tevent.user.model.UserId

case class OrgParticipation(userId: Long,
                            organizationId: Long,
                            participationType: OrgParticipationType)

object OrgParticipation {
  implicit object OrgParticipationNamed extends EntityType[(UserId, OrganizationId)] {
    override val name: String = "OrgParticipation"
  }

  def mapperTo(tuple: (Long, Long, OrgParticipationType)): OrgParticipation =
    OrgParticipation(tuple._1, tuple._2, tuple._3)
}
