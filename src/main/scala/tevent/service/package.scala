package tevent

import zio.Has

package object service {
  type AuthService = Has[AuthService.Service]
  type UsersService = Has[UsersService.Service]
  type EventsService = Has[EventsService.Service]
  type OrganizationsService = Has[OrganizationsService.Service]
  type Services = AuthService with UsersService with EventsService with OrganizationsService
}
