package tevent

import tevent.core.Db
import tevent.organizations.repository._
import tevent.organizations.service.{OrganizationParticipants, Organizations}
import zio.URLayer

package object organizations {
  type Tables = OrganizationsT with OrganizationTagsT with OrgParticipantsT with OrgParticipationRequestsT
  type Repositories = OrganizationsRepository with OrganizationParticipantsRepository with OrganizationRequestsRepository
  type Services = Organizations with OrganizationParticipants

  val repositories: URLayer[Db with Tables, Repositories] =
    SlickOrganizationsRepository.live ++ SlickOrganizationParticipantsRepository.live ++ SlickOrganizationRequestsRepository.live
  val services: URLayer[Repositories, Services] =
    Organizations.live ++ OrganizationParticipants.live
}
