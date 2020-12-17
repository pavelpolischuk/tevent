package tevent.events

import tevent.core.{AccessError, DomainError, ValidationError}
import tevent.events.model.{Event, EventParticipation, OfflineParticipant, OnlineParticipant}
import tevent.events.service.{EventParticipants, Events}
import tevent.mock.{EventParticipantsRepositoryMock, EventsRepositoryMock, NotificationServiceMock, OrganizationParticipantsMock}
import tevent.organizations.model.{OrgManager, Organization}
import tevent.user.model.User
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation

import java.time.ZonedDateTime

object EventsServiceTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("Events")(

    testM("get event") {
      val repository = EventsRepositoryMock.GetById(equalTo(1L), Expectation.value(Some(event)))
      val service = repository ++ orgParticipants ++ notification >>> Events.live
      for {
        gotten <- Events.get(event.id).provideSomeLayer(service)
      } yield assert(gotten)(equalTo(event))
    },

    testM("get none for not existing event") {
      val repository = EventsRepositoryMock.GetById(equalTo(2L), Expectation.value(None))
      val service = repository ++ orgParticipants ++ notification >>> Events.live
      val action = Events.get(2).provideSomeLayer(service).run
      assertM(action)(fails(isSubtype[DomainError](anything)))
    },

    testM("join event if has seats") {
      val events = EventsRepositoryMock.GetById(equalTo(event.id), Expectation.value(Some(event)))
      val participants = EventParticipantsRepositoryMock.Check(equalTo((user2.id, event.id)), Expectation.value(None)) &&
        EventParticipantsRepositoryMock.GetParticipants(equalTo(event.id), Expectation.value(List((user1, OnlineParticipant)))) ++
        EventParticipantsRepositoryMock.Add(equalTo(participation))

      val service = events ++ participants ++ orgParticipants >>> EventParticipants.live
      for {
        _ <- EventParticipants.joinEvent(participation).provideSomeLayer(service)
      } yield assertCompletes
    },

    testM("fail join event if has not seats") {
      val events = EventsRepositoryMock.GetById(equalTo(event.id), Expectation.value(Some(event)))
      val participants = EventParticipantsRepositoryMock.GetParticipants(equalTo(event.id), Expectation.value(List((user1, OfflineParticipant)))) &&
        EventParticipantsRepositoryMock.Check(equalTo((user2.id, event.id)), Expectation.value(None))
      val service = events ++ participants ++ orgParticipants >>> EventParticipants.live
      val action = EventParticipants.joinEvent(participation).provideSomeLayer(service).run
      assertM(action)(fails(isSubtype[ValidationError](anything)))
    },

    testM("create event and notify if has rights") {
      val repository = EventsRepositoryMock.Add(equalTo(event.copy(id = -1)), Expectation.value(1L))
      val notification = NotificationServiceMock.NotifySubscribers(equalTo(event))
      val participants = OrganizationParticipantsMock.CheckUser(equalTo((user1.id, event.organizationId, OrgManager)), Expectation.unit)
      val service = participants ++ repository ++ notification >>> Events.live
      for {
        result <- Events.create(user1.id, event.copy(id = -1)).provideSomeLayer(service)
      } yield assert(result)(equalTo(event))
    },

    testM("fail create event if has not rights") {
      val repository = EventsRepositoryMock.Empty
      val participants = OrganizationParticipantsMock.CheckUser(equalTo((user2.id, organization.id, OrgManager)), Expectation.failure(AccessError))
      val service = participants ++ repository ++ notification >>> Events.live
      val action = Events.create(user2.id, event.copy(id = -1)).provideSomeLayer(service).run
      assertM(action)(fails(equalTo(AccessError)))
    },

  )

  private val user1 = User(1, "N1", "e1", "", 0)
  private val user2 = User(2, "N2", "e2", "", 0)
  private val organization = Organization(1, "Paul Corp.", "pcorp", "Description", List("scala", "dev"))
  private val event = Event(1, organization.id, "Paul Meetup #1", "Meetup Description", ZonedDateTime.now(), Some("Moscow"), Some(1), Some("video"))
  private val participation = EventParticipation(user2.id, event.id, OfflineParticipant)

  private val notification = NotificationServiceMock.Empty
  private val orgParticipants = OrganizationParticipantsMock.Empty
}
