package tevent

import tevent.domain.model.{Event, OrgManager, OrgOwner, OrgSubscriber, Organization, User}
import tevent.http.model.event.EventForm
import tevent.http.model.organization.{OrgParticipationApprove, OrgParticipationData, OrgParticipationForm, OrgParticipationRequest, OrganizationForm}
import tevent.http.model.user.{LoginData, LoginForm, UserData, UserId}
import zio.test.Assertion.{equalTo, isEmpty}
import zio.test.TestAspect.sequential
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.ZonedDateTime

object TEventIntegrationTest extends DefaultRunnableSpec {

  private def httpServer = Main.run(List()).forkManaged.toLayer

  private val user = User(1, "Paul", "paul@g.com", "1234")
  private val userLogin = LoginForm(Some(user.name), user.email, user.secretHash)
  private val userId = UserId(user.id, user.name)
  private val userData = UserData(user.name, user.email)

  private val user2 = User(2, "Phil", "phil@g.com", "2345")
  private val user2Login = LoginForm(Some(user2.name), user2.email, user2.secretHash)
  private val user2Id = UserId(user2.id, user2.name)

  private val organization = Organization(1, "Paul Corp.")
  private val organizationForm = OrganizationForm(organization.name)

  private val memberRequestForm = OrgParticipationForm(OrgManager)
  private val memberRequest = OrgParticipationRequest(user2Id, memberRequestForm.participationType, None)
  private val memberApprove = OrgParticipationApprove(user2.id)

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

        newUser <- HttpClient.get[UserData]("http://localhost:8080/api/v1/user", response.token)
      } yield assert(newUser)(equalTo(userData))
    },

    testM("can add & get organization") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token

        added <- HttpClient.post[OrganizationForm, Organization]("http://localhost:8080/api/v1/organizations", token, organizationForm)
        _ = assert(added)(equalTo(organization))

        gotten <- HttpClient.get[Organization]("http://localhost:8080/api/v1/organizations/1", token)
        forUser <- HttpClient.get[List[OrgParticipationData]]("http://localhost:8080/api/v1/user/organizations", token)
        participation = OrgParticipationData(added, OrgOwner)
      } yield {
        assert(gotten)(equalTo(added))
        assert(forUser)(equalTo(List(participation)))
      }
    },

    testM("can join & request & approve members in organization") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/signin", user2Login)
        token2 = response.token
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token

        _ <- HttpClient.post[OrgParticipationForm, Unit]("http://localhost:8080/api/v1/organizations/1/join", token2, OrgParticipationForm(OrgSubscriber))
        forUser <- HttpClient.get[List[OrgParticipationData]]("http://localhost:8080/api/v1/user/organizations", token2)
        participation = OrgParticipationData(organization, OrgSubscriber)
        _ = assert(forUser)(equalTo(List(participation)))

        _ <- HttpClient.post[String, Unit]("http://localhost:8080/api/v1/organizations/1/leave", token2, "")
        forUser <- HttpClient.get[List[OrgParticipationData]]("http://localhost:8080/api/v1/user/organizations", token2)
        _ = assert(forUser)(equalTo(List.empty))

        _ <- HttpClient.post[OrgParticipationForm, Unit]("http://localhost:8080/api/v1/organizations/1/join", token2, memberRequestForm)
        requests <- HttpClient.get[List[OrgParticipationRequest]]("http://localhost:8080/api/v1/organizations/1/requests", token)
        _ = assert(requests)(equalTo(List(memberRequest)))

        _ <- HttpClient.post[OrgParticipationApprove, Unit]("http://localhost:8080/api/v1/organizations/1/approve", token, memberApprove)
        requests <- HttpClient.get[List[OrgParticipationRequest]]("http://localhost:8080/api/v1/organizations/1/requests", token)
        forUser <- HttpClient.get[List[OrgParticipationData]]("http://localhost:8080/api/v1/user/organizations", token2)
        participation = OrgParticipationData(organization, memberRequest.participationType)
      } yield {
        assert(requests)(isEmpty)
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
