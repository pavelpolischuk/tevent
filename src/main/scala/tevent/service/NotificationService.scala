package tevent.service

import tevent.domain.Named.organizationNamed
import tevent.domain.model.{Event, Organization, User}
import tevent.domain.repository.OrganizationsRepository
import tevent.domain.{DomainError, EntityNotFound, ExecutionError}
import tevent.infrastructure.service.{Email, EmailSender}
import zio.{IO, URLayer, ZIO, ZLayer}


object NotificationService {
  trait Service {
    def notifySubscribers(event: Event): IO[DomainError, Unit]
    def notifyNewEvent(organization: Organization, event: Event, users: List[User]): IO[DomainError, Unit]
  }

  class EmailNotificationService(organizations: OrganizationsRepository.Service,
                                 email: EmailSender.Service) extends NotificationService.Service {

    override def notifySubscribers(event: Event): IO[DomainError, Unit] = for {
      organization <- organizations.getById(event.organizationId).someOrFail(EntityNotFound(event.organizationId))
      users <- organizations.getUsers(organization.id).map(_.map(_._1))
      _ <- notifyNewEvent(organization, event, users)
    } yield ()

    override def notifyNewEvent(organization: Organization, event: Event, users: List[User]): IO[DomainError, Unit] = {
      val subject = s"New event from ${organization.name}"
      val body = {
        val builder = new StringBuilder(
          s"""${event.name} (https://tevent.herokuapp.com/api/v1/events/${event.id})
             |
             |Organizer: ${organization.name} (https://tevent.herokuapp.com/api/v1/organizations/${organization.id})
             |When: ${event.datetime}
             |""".stripIndent)

        event.location.foreach(l => builder.addAll(s"Where: $l\n"))
        event.videoBroadcastLink.foreach(v => builder.addAll(s"Online broadcast: $v\n"))

        builder.result()
      }

      ZIO.foreach(users)(u => email.sendMail(u.email, subject, body))
        .mapError(ExecutionError).unit
    }
  }

  def live: URLayer[OrganizationsRepository with Email, NotificationService] =
    ZLayer.fromServices[OrganizationsRepository.Service, EmailSender.Service, NotificationService.Service](
      new EmailNotificationService(_, _))


  def notifySubscribers(event: Event): ZIO[NotificationService, DomainError, Unit] =
    ZIO.accessM(_.get.notifySubscribers(event))

  def notifyNewEvent(organization: Organization, event: Event, users: List[User]): ZIO[NotificationService, DomainError, Unit] =
    ZIO.accessM(_.get.notifyNewEvent(organization, event, users))
}
