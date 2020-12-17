package tevent

import tevent.core.Db
import tevent.events.repository._
import tevent.events.service.{EventParticipants, Events}
import tevent.notification.Notification
import tevent.organizations.service.OrganizationParticipants
import zio.URLayer

package object events {
  type Tables = EventsT with EventParticipantsT
  type Repositories = EventsRepository with EventParticipantsRepository
  type Services = Events with EventParticipants

  val repositories: URLayer[Db with Tables, Repositories] =
    SlickEventsRepository.live ++ SlickEventParticipantsRepository.live
  val services: URLayer[Repositories with OrganizationParticipants with Notification, Services] =
    Events.live ++ EventParticipants.live
}
