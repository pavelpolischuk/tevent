package tevent.domain

import zio.Has

package object repository {
  type UsersRepository = Has[UsersRepository.Service]
  type EventsRepository = Has[EventsRepository.Service]
  type OrganizationsRepository = Has[OrganizationsRepository.Service]
  type Repositories = UsersRepository with EventsRepository with OrganizationsRepository
}
