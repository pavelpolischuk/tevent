package tevent.organizations.repository

import slick.jdbc.{JdbcProfile, JdbcType}
import tevent.organizations.model.{OrgManager, OrgMember, OrgOwner, OrgParticipationType, OrgSubscriber}

package object tables {
  private[repository] implicit def orgParticipationColumnType(implicit profile: JdbcProfile): JdbcType[OrgParticipationType] = {
    import profile.api._

    MappedColumnType.base[OrgParticipationType, Int](_.value,
      {
        case 100 => OrgSubscriber
        case 200 => OrgMember
        case 300 => OrgManager
        case 400 => OrgOwner
      }
    )
  }

  private[repository] case class PlainOrganization(id: Long, name: String, nick: String, description: String)

  private[repository] object PlainOrganization {
    def mapperTo(tuple: (Long, String, String, String)): PlainOrganization =
      PlainOrganization(tuple._1, tuple._2, tuple._3, tuple._4)
  }
}
