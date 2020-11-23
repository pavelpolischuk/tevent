package tevent.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Organization(id: Long,
                        name: String)

object Organization {
  implicit val organizationEncoder: Encoder[Organization] = deriveEncoder[Organization]
  implicit val organizationDecoder: Decoder[Organization] = deriveDecoder[Organization]

  def mapperTo(tuple: (Long, String)): Organization =
    Organization(tuple._1, tuple._2)
}
