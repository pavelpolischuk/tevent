package tevent.notification

import tevent.core.{DomainError, ExecutionError}
import tevent.events.model.Event
import tevent.organizations.model.Organization
import tevent.organizations.repository.OrganizationParticipantsRepository
import tevent.organizations.service.Organizations
import tevent.user.model.User
import zio.{IO, URLayer, ZIO, ZLayer}

object Notification {
  trait Service {
    def notifySubscribers(event: Event): IO[DomainError, Unit]
    def notifyNewEvent(organization: Organization, event: Event, users: List[User]): IO[DomainError, Unit]
  }

  class EmailNotificationService(organizations: Organizations.Service,
                                 participants: OrganizationParticipantsRepository.Service,
                                 email: Email.Service) extends Notification.Service {

    override def notifySubscribers(event: Event): IO[DomainError, Unit] = for {
      organization <- organizations.get(event.organizationId)
      users <- participants.getParticipants(organization.id).map(_.map(_._1))
      _ <- notifyNewEvent(organization, event, users)
    } yield ()

    override def notifyNewEvent(organization: Organization, event: Event, users: List[User]): IO[DomainError, Unit] = {
      val subject = s"New event from ${organization.name}"
      val body = {
        val builder = new StringBuilder(
          s"""${event.name} (https://tevent.herokuapp.com/api/v1/events/${event.id})

Organizer: ${organization.name} (https://tevent.herokuapp.com/api/v1/organizations/${organization.id})
When: ${event.datetime}
""")

        event.location.foreach(l => builder.addAll(s"Where: $l\n"))
        event.videoBroadcastLink.foreach(v => builder.addAll(s"Online broadcast: $v\n"))

        builder.result()
      }

      ZIO.foreach(users)(u => email.sendMail(u.email, subject, body))
        .mapError(ExecutionError).unit
    }
  }


  def live: URLayer[Organizations with OrganizationParticipantsRepository with Email, Notification] =
    ZLayer.fromServices[Organizations.Service, OrganizationParticipantsRepository.Service, Email.Service, Notification.Service](
      new EmailNotificationService(_, _, _))


  def notifySubscribers(event: Event): ZIO[Notification, DomainError, Unit] =
    ZIO.accessM(_.get.notifySubscribers(event))

  def notifyNewEvent(organization: Organization, event: Event, users: List[User]): ZIO[Notification, DomainError, Unit] =
    ZIO.accessM(_.get.notifyNewEvent(organization, event, users))
}
