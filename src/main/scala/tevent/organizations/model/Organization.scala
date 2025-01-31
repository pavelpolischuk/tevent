package tevent.organizations.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Organization(id: Long,
                        name: String,
                        nick: String,
                        description: String,
                        tags: List[String]) {

  def typedId: OrganizationId = OrganizationId(id)
}

object Organization {
  implicit val organizationEncoder: Encoder[Organization] = deriveEncoder[Organization]
  implicit val organizationDecoder: Decoder[Organization] = deriveDecoder[Organization]
}
