package tevent.organizations.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import tevent.core.EntityType

case class Organization(id: Long,
                        name: String,
                        nick: String,
                        description: String,
                        tags: List[String])

object Organization {
  implicit val organizationEncoder: Encoder[Organization] = deriveEncoder[Organization]
  implicit val organizationDecoder: Decoder[Organization] = deriveDecoder[Organization]

  implicit val organizationNamed: EntityType[Organization] = new EntityType[Organization] {
    override val name: String = "Organization"
  }
}
