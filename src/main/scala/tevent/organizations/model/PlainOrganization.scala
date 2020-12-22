package tevent.organizations.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class PlainOrganization(id: Long, name: String, nick: String, description: String)

object PlainOrganization {
  implicit val plainOrgEncoder: Encoder[PlainOrganization] = deriveEncoder[PlainOrganization]
  implicit val plainOrgDecoder: Decoder[PlainOrganization] = deriveDecoder[PlainOrganization]

  def mapperTo(tuple: (Long, String, String, String)): PlainOrganization =
    PlainOrganization(tuple._1, tuple._2, tuple._3, tuple._4)
}
