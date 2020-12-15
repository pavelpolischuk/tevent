package tevent.organizations.model

import tevent.core.EntityType

case class OrgParticipation(userId: Long,
                            organizationId: Long,
                            participationType: OrgParticipationType)

object OrgParticipation {
  implicit val orgParticipationNamed: EntityType[OrgParticipation] = new EntityType[OrgParticipation] {
    override val name: String = "OrgParticipation"
  }

  def mapperTo(tuple: (Long, Long, OrgParticipationType)): OrgParticipation =
    OrgParticipation(tuple._1, tuple._2, tuple._3)
}
