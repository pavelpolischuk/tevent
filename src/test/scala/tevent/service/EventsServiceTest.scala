package tevent.service

import tevent.domain.{AccessError, ValidationError}
import tevent.domain.model._
import tevent.mock.{EventsRepositoryMock, InMemoryParticipationService, NotificationServiceMock}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation

import java.time.ZonedDateTime

object EventsServiceTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("EventsService")(

    testM("get event") {
      val repository = EventsRepositoryMock.GetById(equalTo(1L), Expectation.value(Some(event)))
      val service = participations ++ notification ++ repository >>> EventsService.live
      for {
        gotten <- EventsService.get(event.id).provideSomeLayer(service)
      } yield assert(gotten)(isSome(equalTo(event)))
    },

    testM("get none for not existing event") {
      val repository = EventsRepositoryMock.GetById(equalTo(2L), Expectation.value(None))
      val service = participations ++ notification ++ repository >>> EventsService.live
      for {
        gotten <- EventsService.get(2).provideSomeLayer(service)
      } yield assert(gotten)(isNone)
    },

    testM("join event if has seats") {
      val repository =
        EventsRepositoryMock.GetById(equalTo(event.id), Expectation.value(Some(event))) &&
        EventsRepositoryMock.CheckUser(equalTo((user2.id, event.id)), Expectation.value(None)) &&
        EventsRepositoryMock.GetUsers(equalTo(event.id), Expectation.value(List((user1, OnlineParticipant)))) ++
        EventsRepositoryMock.AddUser(equalTo(participation))
      val service = participations ++ notification ++ repository >>> EventsService.live
      for {
        _ <- EventsService.joinEvent(participation).provideSomeLayer(service)
      } yield assertCompletes
    },

    testM("fail join event if has not seats") {
      val repository =
        EventsRepositoryMock.GetUsers(equalTo(event.id), Expectation.value(List((user1, OfflineParticipant)))) &&
        EventsRepositoryMock.GetById(equalTo(event.id), Expectation.value(Some(event))) &&
        EventsRepositoryMock.CheckUser(equalTo((user2.id, event.id)), Expectation.value(None))
      val service = participations ++ notification ++ repository >>> EventsService.live
      val action = EventsService.joinEvent(participation).provideSomeLayer(service).run
      assertM(action)(fails(isSubtype[ValidationError](anything)))
    },

    testM("create event and notify if has rights") {
      val repository = EventsRepositoryMock.Add(equalTo(event.copy(id = -1)), Expectation.value(1L))
      val notification = NotificationServiceMock.NotifySubscribers(equalTo(event))
      val service = participations ++ notification ++ repository >>> EventsService.live
      for {
        result <- EventsService.create(user1.id, organization.id, event.name, event.datetime, event.location, event.capacity, event.videoBroadcastLink).provideSomeLayer(service)
      } yield assert(result)(equalTo(event))
    },

    testM("fail create event if has not rights") {
      val repository = EventsRepositoryMock.Empty
      val service = participations ++ notification ++ repository >>> EventsService.live
      val action = EventsService.create(user2.id, organization.id, event.name, event.datetime, event.location, event.capacity, event.videoBroadcastLink).provideSomeLayer(service).run
      assertM(action)(fails(equalTo(AccessError)))
    },

  )

  private val user1 = User(1, "N1", "e1", "", 0)
  private val user2 = User(2, "N2", "e2", "", 0)
  private val organization = Organization(1, "Paul Corp.", List("scala", "dev"))
  private val event = Event(1, organization.id, "Paul Meetup #1", ZonedDateTime.now(), Some("Moscow"), Some(1), Some("video"))
  private val participation = EventParticipation(user2.id, event.id, OfflineParticipant)

  private val notification = NotificationServiceMock.Empty
  private val participations = InMemoryParticipationService.layer(user1, organization, List((user1, organization, OrgManager)))
}
