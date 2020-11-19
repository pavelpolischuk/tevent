package tevent.infrastructure.repository

import zio.Has

package object tables {
  type UsersT = Has[UsersTable]
  type OrganizationsT = Has[OrganizationsTable]
  type EventsT = Has[EventsTable]
  type EventParticipantsT = Has[EventParticipantsTable]
  type OrgParticipantsT = Has[OrgParticipantsTable]
}
