package tevent.organizations.model

import tevent.core.EntityType

case class OrgParticipationRequest(userId: Long,
                                   organizationId: Long,
                                   participationType: OrgParticipationType,
                                   fromUserId: Long)  {

  def toParticipation: OrgParticipation = OrgParticipation(userId, organizationId, participationType)

  val tupled: (Long, Long, OrgParticipationType, Long) = (userId, organizationId, participationType, fromUserId)
}


object OrgParticipationRequest {
  implicit val orgParticipationRequestNamed: EntityType[OrgParticipationRequest] = new EntityType[OrgParticipationRequest] {
    override val name: String = "OrgParticipationRequest"
  }

  def apply(participation: OrgParticipation): OrgParticipationRequest =
    OrgParticipationRequest(participation.userId, participation.organizationId, participation.participationType, participation.userId)

  def apply(participation: OrgParticipation, fromUserId: Long): OrgParticipationRequest =
    OrgParticipationRequest(participation.userId, participation.organizationId, participation.participationType, fromUserId)

  def mapperTo(tuple: (Long, Long, OrgParticipationType, Long)): OrgParticipationRequest =
    OrgParticipationRequest(tuple._1, tuple._2, tuple._3, tuple._4)
}
