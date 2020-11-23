package tevent.domain.model

case class OrgParticipation(userId: Long,
                            organizationId: Long,
                            participationType: OrgParticipationType)
