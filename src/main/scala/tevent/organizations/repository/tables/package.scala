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
}
