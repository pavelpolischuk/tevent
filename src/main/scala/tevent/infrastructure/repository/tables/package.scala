package tevent.infrastructure.repository

import slick.jdbc.{JdbcProfile, JdbcType}
import tevent.domain.model.{EventParticipationType, EventSubscriber, OfflineParticipant, OnlineParticipant, OrgManager, OrgMember, OrgOwner, OrgParticipationType, OrgSubscriber}
import zio.Has

package object tables {
  type UsersT = Has[UsersTable]
  type OrganizationsT = Has[OrganizationsTable]
  type EventsT = Has[EventsTable]
  type EventParticipantsT = Has[EventParticipantsTable]
  type OrgParticipantsT = Has[OrgParticipantsTable]
  type OrgParticipationRequestsT = Has[OrgParticipationRequestsTable]
  type OrganizationTagsT = Has[OrganizationTagsTable]

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

  private[repository] implicit def eventParticipationColumnType(implicit profile: JdbcProfile): JdbcType[EventParticipationType] = {
    import profile.api._

    MappedColumnType.base[EventParticipationType, Int](
      {
        case EventSubscriber => 0
        case OnlineParticipant => 1
        case OfflineParticipant => 2
      },
      {
        case 0 => EventSubscriber
        case 1 => OnlineParticipant
        case 2 => OfflineParticipant
      }
    )
  }

  private[repository] case class PlainOrganization(id: Long, name: String)

  private[repository] object PlainOrganization {
    def mapperTo(tuple: (Long, String)): PlainOrganization =
      PlainOrganization(tuple._1, tuple._2)
  }
}
