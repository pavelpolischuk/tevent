package tevent.organizations

import tevent.organizations.repository.tables.{OrgParticipantsTable, OrgParticipationRequestsTable, OrganizationTagsTable, OrganizationsTable}
import zio.Has

package object repository {
  type OrganizationsT = Has[OrganizationsTable]
  type OrgParticipantsT = Has[OrgParticipantsTable]
  type OrgParticipationRequestsT = Has[OrgParticipationRequestsTable]
  type OrganizationTagsT = Has[OrganizationTagsTable]

  type OrganizationsRepository = Has[OrganizationsRepository.Service]
  type OrganizationParticipantsRepository = Has[OrganizationParticipantsRepository.Service]
  type OrganizationRequestsRepository = Has[OrganizationRequestsRepository.Service]
}
