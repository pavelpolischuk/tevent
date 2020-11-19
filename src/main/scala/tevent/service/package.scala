package tevent

import zio.Has

package object service {
  type UsersService = Has[UsersService.Service]
  type EventsService = Has[EventsService.Service]
  type OrganizationsService = Has[OrganizationsService.Service]
  type Services = UsersService with EventsService with OrganizationsService
}
