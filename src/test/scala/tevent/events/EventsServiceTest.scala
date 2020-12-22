package tevent.events

import tevent.core.{AccessError, DomainError, ValidationError}
import tevent.events.mock.{EventParticipantsRepositoryMock, EventsRepositoryMock}
import tevent.events.model.{EventId, OfflineParticipant, OnlineParticipant}
import tevent.events.service.{EventParticipants, Events}
import tevent.helpers.TestData._
import tevent.organizations.mock.OrganizationParticipantsMock
import tevent.notification.mock.NotificationServiceMock
import tevent.organizations.model.{OrgManager, OrganizationId}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation

object EventsServiceTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("Events")(

    testM("get event") {
      val repository = EventsRepositoryMock.GetById(equalTo(1L), Expectation.value(Some((event, plainOrganization))))
      val service = repository ++ orgParticipants ++ notification >>> Events.live
      for {
        gotten <- Events.get(event.typedId).provideSomeLayer(service)
      } yield assert(gotten)(equalTo(eventData))
    },

    testM("get none for not existing event") {
      val repository = EventsRepositoryMock.GetById(equalTo(2L), Expectation.value(None))
      val service = repository ++ orgParticipants ++ notification >>> Events.live
      val action = Events.get(EventId(2L)).provideSomeLayer(service).run
      assertM(action)(fails(isSubtype[DomainError](anything)))
    },

    testM("join event if has seats") {
      val events = EventsRepositoryMock.GetById(equalTo(event.id), Expectation.value(Some((event, plainOrganization))))
      val participants = EventParticipantsRepositoryMock.Check(equalTo((user2.id, event.id)), Expectation.value(None)) &&
        EventParticipantsRepositoryMock.GetParticipants(equalTo(event.id), Expectation.value(List((user, OnlineParticipant)))) ++
        EventParticipantsRepositoryMock.Add(equalTo(participation2))

      val service = events ++ participants ++ orgParticipants >>> EventParticipants.live
      for {
        _ <- EventParticipants.joinEvent(participation2).provideSomeLayer(service)
      } yield assertCompletes
    },

    testM("fail join event if has not seats") {
      val events = EventsRepositoryMock.GetById(equalTo(event.id), Expectation.value(Some((event, plainOrganization))))
      val participants = EventParticipantsRepositoryMock.GetParticipants(equalTo(event.id), Expectation.value(List((user, OfflineParticipant)))) &&
        EventParticipantsRepositoryMock.Check(equalTo((user2.id, event.id)), Expectation.value(None))
      val service = events ++ participants ++ orgParticipants >>> EventParticipants.live
      val action = EventParticipants.joinEvent(participation2).provideSomeLayer(service).run
      assertM(action)(fails(isSubtype[ValidationError](anything)))
    },

    testM("create event and notify if has rights") {
      val repository = EventsRepositoryMock.Add(equalTo(event.copy(id = -1)), Expectation.value(1L))
      val notification = NotificationServiceMock.NotifySubscribers(equalTo(event))
      val participants = OrganizationParticipantsMock.CheckUser(equalTo((user.typedId, OrganizationId(event.organizationId), OrgManager)), Expectation.unit)
      val service = participants ++ repository ++ notification >>> Events.live
      for {
        result <- Events.create(user.typedId, event.copy(id = -1)).provideSomeLayer(service)
      } yield assert(result)(equalTo(event))
    },

    testM("fail create event if has not rights") {
      val repository = EventsRepositoryMock.Empty
      val participants = OrganizationParticipantsMock.CheckUser(equalTo((user2.typedId, OrganizationId(organization.id), OrgManager)), Expectation.failure(AccessError))
      val service = participants ++ repository ++ notification >>> Events.live
      val action = Events.create(user2.typedId, event.copy(id = -1)).provideSomeLayer(service).run
      assertM(action)(fails(equalTo(AccessError)))
    }
  )

  private val notification = NotificationServiceMock.Empty
  private val orgParticipants = OrganizationParticipantsMock.Empty
}
