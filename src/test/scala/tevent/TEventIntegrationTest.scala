package tevent

import tevent.domain.model.{Event, OrgOwner, Organization, User}
import tevent.http.model._
import zio.test.Assertion.equalTo
import zio.test.TestAspect.sequential
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.ZonedDateTime

object TEventIntegrationTest extends DefaultRunnableSpec {

  private def httpServer = Main.run(List()).forkManaged.toLayer

  private val user = User(1, "Paul", "paul@g.com", "1234")
  private val userLogin = LoginForm(Some(user.name), user.email, user.secretHash)
  private val userData = UserData(user.name, user.email)

  private val organization = Organization(1, "Paul Corp.")
  private val organizationForm = OrganizationForm(organization.name)

  private val event = Event(1, organization.id, "Paul Meetup #1", ZonedDateTime.now(), Some("Moscow"), Some(128), None)
  private val eventForm = EventForm(event.id, event.name, event.datetime, event.location, event.capacity, event.videoBroadcastLink)


  def spec: ZSpec[TestEnvironment, Any] = suite("A started Main")(

    testM("should be healthy") {
      for {
        response <- HttpClient.get[String]("http://localhost:8080/api/v1/health")
      } yield assert(response)(equalTo("OK"))
    },

    testM("can sign-in, login & get user") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/signin", userLogin)
        userId1 = response.token.split('-')(1).toLong
        _ = assert(userId1)(equalTo(user.id))

        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        userId2 = response.token.split('-')(1).toLong
        _ = assert(userId2)(equalTo(user.id))

        newUser <- HttpClient.get[UserData]("http://localhost:8080/api/v1/users/1", response.token)
      } yield assert(newUser)(equalTo(userData))
    },

    testM("can add & get organization") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token

        added <- HttpClient.post[OrganizationForm, Organization]("http://localhost:8080/api/v1/organizations", token, organizationForm)
        _ = assert(added)(equalTo(organization))

        gotten <- HttpClient.get[Organization]("http://localhost:8080/api/v1/organizations/1", token)
        forUser <- HttpClient.get[List[OrgParticipation]]("http://localhost:8080/api/v1/users/1/organizations", token)
        participation = OrgParticipation(added, OrgOwner)
      } yield {
        assert(gotten)(equalTo(added))
        assert(forUser)(equalTo(List(participation)))
      }
    },

    testM("can add & get event") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token

        added <- HttpClient.post[EventForm, Event]("http://localhost:8080/api/v1/events", token, eventForm)
        _ = assert(added)(equalTo(event))

        gotten <- HttpClient.get[Event]("http://localhost:8080/api/v1/events/1", token)
      } yield {
        assert(gotten)(equalTo(event))
      }
    },

  ).provideCustomLayerShared(httpServer) @@ sequential
}
