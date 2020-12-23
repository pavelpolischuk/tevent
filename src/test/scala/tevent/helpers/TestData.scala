package tevent.helpers

import tevent.events.dto.{EventData, EventForm, EventParticipationData, EventParticipationForm, EventUserParticipationData}
import tevent.events.model.{Event, EventParticipation, EventSubscriber, OfflineParticipant}
import tevent.organizations.dto.{OrgParticipationApprove, OrgParticipationForm, OrgParticipationRequest, OrgUserParticipationData, OrganizationForm}
import tevent.organizations.model.{OrgManager, OrgOwner, Organization, PlainOrganization}
import tevent.user.dto.{GoogleLoginForm, LoginForm, SigninForm, UserData}
import tevent.user.model.{User, UserToken}

import java.time.ZonedDateTime

//noinspection TypeAnnotation
object TestData {
  val user = User(1, "Paul", "paul@g.com", Some("1234"), None, 0)
  val userSignin = SigninForm(user.name, user.email, user.secretHash.get)
  val userLogin = LoginForm(user.email, user.secretHash.get)
  val userData = UserData(user.id, user.name, user.email)
  val userToken = UserToken("123", user.id, 123456)
  val googleToken = GoogleLoginForm("google data")

  val user2 = User(2, "Phil", "phil@g.com", Some("2345"), None, 0)
  val user2Login = LoginForm(user2.email, user2.secretHash.get)
  val user2Signin = SigninForm(user2.name, user2.email, user2.secretHash.get)
  val user2Data = UserData(user2.id, user2.name, user2.email)

  val organization = Organization(1, "Paul Corp.", "pcorp", "Paul Description", List("scala", "dev"))
  val organizationForm = OrganizationForm(organization.name, organization.nick, organization.description, organization.tags)
  val plainOrganization = PlainOrganization(organization.id, organization.name, organization.nick, organization.description)

  val ownerParticipation = OrgUserParticipationData(userData, OrgOwner)
  val memberRequestForm = OrgParticipationForm(OrgManager)
  val memberRequest = OrgParticipationRequest(user2Data, memberRequestForm.participationType, None)
  val memberApprove = OrgParticipationApprove(user2.id)
  val memberParticipation = OrgUserParticipationData(user2Data, memberRequestForm.participationType)

  val event = Event(1, organization.id, "Paul Meetup #1", "Meetup Description", ZonedDateTime.now(), Some("Moscow"), Some(1), None, List("dev"))
  val eventData = EventData(event.id, plainOrganization, event.name, event.description, event.datetime, event.location, event.capacity, event.videoBroadcastLink, event.tags)
  val eventForm = EventForm(event.id, event.name, event.description, event.datetime, event.location, event.capacity, event.videoBroadcastLink, event.tags)

  val offlineJoin = EventParticipationForm(OfflineParticipant)
  val offlineParticipation = EventParticipationData(eventData, offlineJoin.participationType)
  val offlineUser = EventUserParticipationData(userData, offlineJoin.participationType)
  val participation2 = EventParticipation(user2.id, event.id, OfflineParticipant)

  val subscribeJoin = EventParticipationForm(EventSubscriber)
  val subscribedParticipation = EventParticipationData(eventData, subscribeJoin.participationType)
  val subscribedUser = EventUserParticipationData(user2Data, subscribeJoin.participationType)
}
