package tevent.helpers

import tevent.events.dto.{EventForm, EventParticipationData, EventParticipationForm, EventUserParticipationData}
import tevent.events.model.{Event, EventParticipation, EventSubscriber, OfflineParticipant}
import tevent.organizations.dto.{OrgParticipationApprove, OrgParticipationForm, OrgParticipationRequest, OrgUserParticipationData, OrganizationForm}
import tevent.organizations.model.{OrgManager, OrgOwner, Organization}
import tevent.user.dto.{GoogleLoginForm, LoginForm, UserData, UserId}
import tevent.user.model.{User, UserToken}

import java.time.ZonedDateTime

//noinspection TypeAnnotation
object TestData {
  val user = User(1, "Paul", "paul@g.com", Some("1234"), None, 0)
  val userLogin = LoginForm(Some(user.name), user.email, user.secretHash.get)
  val userId = UserId(user.id, user.name)
  val userData = UserData(user.name, user.email)
  val userToken = UserToken("123", user.id, 123456)
  val googleToken = GoogleLoginForm("google data")

  val user2 = User(2, "Phil", "phil@g.com", Some("2345"), None, 0)
  val user2Login = LoginForm(Some(user2.name), user2.email, user2.secretHash.get)
  val user2Id = UserId(user2.id, user2.name)

  val organization = Organization(1, "Paul Corp.", "pcorp", "Paul Description", List("scala", "dev"))
  val organizationForm = OrganizationForm(organization.name, organization.nick, organization.description, organization.tags)

  val ownerParticipation = OrgUserParticipationData(userId, OrgOwner)
  val memberRequestForm = OrgParticipationForm(OrgManager)
  val memberRequest = OrgParticipationRequest(user2Id, memberRequestForm.participationType, None)
  val memberApprove = OrgParticipationApprove(user2.id)
  val memberParticipation = OrgUserParticipationData(user2Id, memberRequestForm.participationType)

  val event = Event(1, organization.id, "Paul Meetup #1", "Meetup Description", ZonedDateTime.now(), Some("Moscow"), Some(1), None, List("dev"))
  val eventForm = EventForm(event.id, event.name, event.description, event.datetime, event.location, event.capacity, event.videoBroadcastLink, event.tags)

  val offlineJoin = EventParticipationForm(OfflineParticipant)
  val offlineParticipation = EventParticipationData(event, offlineJoin.participationType)
  val offlineUser = EventUserParticipationData(userId, offlineJoin.participationType)
  val participation2 = EventParticipation(user2.id, event.id, OfflineParticipant)

  val subscribeJoin = EventParticipationForm(EventSubscriber)
  val subscribedParticipation = EventParticipationData(event, subscribeJoin.participationType)
  val subscribedUser = EventUserParticipationData(user2Id, subscribeJoin.participationType)
}
