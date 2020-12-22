package tevent.organizations.model

import tevent.core.EntityType

case class OrganizationId(id: Long)

object OrganizationId {
  implicit object OrganizationEntity extends EntityType[OrganizationId] {
    override val name: String = "Organization"
  }
}
