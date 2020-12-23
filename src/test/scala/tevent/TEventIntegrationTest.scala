package tevent

import tevent.events.dto.{EventData, EventForm, EventParticipationData, EventParticipationForm, EventUserParticipationData}
import tevent.events.model.Event
import tevent.helpers.HttpClient
import tevent.helpers.TestData._
import tevent.main.Environments.testEnvironment
import tevent.main.Server
import tevent.organizations.dto._
import tevent.organizations.model.{OrgOwner, OrgSubscriber, Organization}
import tevent.user.dto.{LoginData, LoginForm, SigninForm, UserData}
import zio.console.Console
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{ExitCode, Fiber, Has, ZLayer}

object TEventIntegrationTest extends DefaultRunnableSpec {

  def spec: ZSpec[TestEnvironment, Any] = suite("A started Main")(

    testM("should be healthy") {
      for {
        response <- HttpClient.get[String]("http://localhost:8080/api/v1/health")
      } yield assert(response)(equalTo("OK"))
    },

    testM("can sign-in, login & get user") {
      for {
        response <- HttpClient.post[SigninForm, LoginData]("http://localhost:8080/api/v1/signin", userSignin)
        userId1 = response.token.split('.')(1).toLong

        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        userId2 = response.token.split('.')(1).toLong

        newUser <- HttpClient.get[UserData]("http://localhost:8080/api/v1/user", response.token)
      } yield {
        assert(userId1)(equalTo(user.id)) &&
        assert(userId2)(equalTo(user.id)) &&
        assert(newUser)(equalTo(userData))
      }
    },

    testM("can add & get organization") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token

        added <- HttpClient.post[OrganizationForm, Organization]("http://localhost:8080/api/v1/organizations", token, organizationForm)

        found <- HttpClient.get[List[Organization]]("http://localhost:8080/api/v1/organizations?tags=scala+dev", token)
        gotten <- HttpClient.get[Organization]("http://localhost:8080/api/v1/organizations/1", token)
        forUser <- HttpClient.get[List[OrgParticipationData]]("http://localhost:8080/api/v1/user/organizations", token)
        participation = OrgParticipationData(added, OrgOwner)
      } yield {
        assert(added)(equalTo(organization)) &&
        assert(found)(equalTo(List(organization))) &&
        assert(gotten)(equalTo(added)) &&
        assert(forUser)(equalTo(List(participation)))
      }
    },

    testM("can join & leave organization") {
      for {
        response <- HttpClient.post[SigninForm, LoginData]("http://localhost:8080/api/v1/signin", user2Signin)
        token2 = response.token

        _ <- HttpClient.post[OrgParticipationForm, Unit]("http://localhost:8080/api/v1/organizations/1/join", token2, OrgParticipationForm(OrgSubscriber))
        forUser <- HttpClient.get[List[OrgParticipationData]]("http://localhost:8080/api/v1/user/organizations", token2)
        participation = OrgParticipationData(organization, OrgSubscriber)

        _ <- HttpClient.post[String, Unit]("http://localhost:8080/api/v1/organizations/1/leave", token2, "")
        forUser2 <- HttpClient.get[List[OrgParticipationData]]("http://localhost:8080/api/v1/user/organizations", token2)
      } yield {
        assert(forUser)(equalTo(List(participation))) &&
        assert(forUser2)(isEmpty)
      }
    },

    testM("can request & approve members in organization") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", user2Login)
        token2 = response.token
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token

        _ <- HttpClient.post[OrgParticipationForm, Unit]("http://localhost:8080/api/v1/organizations/1/join", token2, memberRequestForm)
        requests <- HttpClient.get[List[OrgParticipationRequest]]("http://localhost:8080/api/v1/organizations/1/requests", token)

        _ <- HttpClient.post[OrgParticipationApprove, Unit]("http://localhost:8080/api/v1/organizations/1/approve", token, memberApprove)
        requestsAfterApprove <- HttpClient.get[List[OrgParticipationRequest]]("http://localhost:8080/api/v1/organizations/1/requests", token)
        user2Organizations <- HttpClient.get[List[OrgParticipationData]]("http://localhost:8080/api/v1/user/organizations", token2)
        users <- HttpClient.get[List[OrgUserParticipationData]]("http://localhost:8080/api/v1/organizations/1/users", token)
        participation = OrgParticipationData(organization, memberRequest.participationType)
      } yield {
        assert(requests)(equalTo(List(memberRequest))) &&
        assert(requestsAfterApprove)(isEmpty) &&
        assert(user2Organizations)(equalTo(List(participation))) &&
        assert(users)(hasSameElements(Seq(ownerParticipation, memberParticipation)))
      }
    },

    testM("can add & get event") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token

        added <- HttpClient.post[EventForm, Event]("http://localhost:8080/api/v1/events", token, eventForm)

        gotten <- HttpClient.get[EventData]("http://localhost:8080/api/v1/events/1", token)
        forOrganization <- HttpClient.get[List[EventData]]("http://localhost:8080/api/v1/events?organization=1", token)
      } yield {
        assert(added)(equalTo(event)) &&
        assert(gotten)(equalTo(eventData)) &&
        assert(forOrganization)(equalTo(List(eventData)))
      }
    },

    testM("can join & get event") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", user2Login)
        token2 = response.token

        _ <- HttpClient.post[EventParticipationForm, Unit]("http://localhost:8080/api/v1/events/1/join", token, offlineJoin)
        user1Events <- HttpClient.get[List[EventParticipationData]]("http://localhost:8080/api/v1/user/events", token)

        _ <- HttpClient.post[EventParticipationForm, Unit]("http://localhost:8080/api/v1/events/1/join", token2, subscribeJoin)
        events <- HttpClient.get[List[EventParticipationData]]("http://localhost:8080/api/v1/user/events", token2)
        users <- HttpClient.get[List[EventUserParticipationData]]("http://localhost:8080/api/v1/events/1/users", token)

        badRequest <- HttpClient.post[EventParticipationForm, Unit]("http://localhost:8080/api/v1/events/1/join", token2, offlineJoin).isFailure

        _ <- HttpClient.post[String, Unit]("http://localhost:8080/api/v1/events/1/leave", token2, "")
        users2 <- HttpClient.get[List[EventUserParticipationData]]("http://localhost:8080/api/v1/events/1/users", token)
        events2 <- HttpClient.get[List[EventParticipationData]]("http://localhost:8080/api/v1/user/events", token2)
      } yield {
        assert(user1Events)(equalTo(List(offlineParticipation))) &&
        assert(events)(equalTo(List(subscribedParticipation))) &&
        assert(users)(hasSameElements(Seq(offlineUser, subscribedUser))) &&
        assert(badRequest)(isTrue) &&
        assert(users2)(equalTo(List(offlineUser))) &&
        assert(events2)(isEmpty)
      }
    },

    testM("can remove user") {
      for {
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", userLogin)
        token = response.token
        response <- HttpClient.post[LoginForm, LoginData]("http://localhost:8080/api/v1/login", user2Login)
        token2 = response.token

        _ <- HttpClient.post[EventParticipationForm, Unit]("http://localhost:8080/api/v1/events/1/join", token2, subscribeJoin)
        eventUsersBefore <- HttpClient.get[List[EventUserParticipationData]]("http://localhost:8080/api/v1/events/1/users", token)

        _ <- HttpClient.delete("http://localhost:8080/api/v1/user", token2)
        getError <- HttpClient.get[UserData]("http://localhost:8080/api/v1/user", token2).cause
        eventUsers <- HttpClient.get[List[EventUserParticipationData]]("http://localhost:8080/api/v1/events/1/users", token)
        orgUsers <- HttpClient.get[List[OrgUserParticipationData]]("http://localhost:8080/api/v1/organizations/1/users", token)
      } yield {
        assert(getError.failureOption)(isSome) &&
        assert(eventUsersBefore)(hasSameElements(List(offlineUser, subscribedUser))) &&
        assert(eventUsers)(equalTo(List(offlineUser))) &&
        assert(orgUsers)(equalTo(List(ownerParticipation)))
      }
    },

  ).provideCustomLayerShared(httpServer) @@ sequential

  private def httpServer: ZLayer[Any with Console, Nothing, Has[Fiber.Runtime[Nothing, ExitCode]]] =
    Server.runServer.provideLayer(testEnvironment).exitCode.forkManaged.toLayer
}
