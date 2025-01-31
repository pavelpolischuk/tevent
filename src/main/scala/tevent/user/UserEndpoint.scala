package tevent.user

import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import sttp.model.Header
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import tevent.core.ErrorMapper.errorResponse
import tevent.events.dto.EventParticipationData
import tevent.events.service.Events
import tevent.organizations.dto.{OrgParticipationData, OwnOrgParticipationRequest}
import tevent.organizations.service.{OrganizationParticipants, Organizations}
import tevent.user.dto.{LoginData, SecretForm, UserData, UserForm}
import tevent.user.model.User
import tevent.user.service.{Auth, Users}
import zio._
import zio.interop.catz.taskConcurrentInstance

final class UserEndpoint[R <: Users with Auth with Organizations with OrganizationParticipants with Events] {
  type Task[A] = RIO[R, A]

  private val dsl = Http4sDsl[Task]
  import dsl._

  private val httpRoutes = AuthedRoutes.of[User, Task] {
    case GET -> Root as user => Ok(UserData.mapperTo(user))
    case request@PUT -> Root as user => request.req.decode[UserForm] { form =>
      Users.update(user.typedId, form.name, form.email).foldM(errorResponse, Ok(_))
    }
    case DELETE -> Root as user =>
      Users.remove(user.typedId).foldM(errorResponse, Ok(_))

    case POST -> Root / "revoke" as user =>
      Auth.revokeTokens(user.id).foldM(errorResponse, Ok(_))
    case request@PUT -> Root / "secret" as user => request.req.decode[SecretForm] { form =>
      Auth.changeSecret(user.id, form.secret).foldM(
        failure = errorResponse,
        success = token => Ok(LoginData("Secret changed, use new token", token.signedString)))
    }

    case GET -> Root / "events" as user =>
      Events.getByUser(user.typedId).foldM(errorResponse,
        p => Ok(p.map(EventParticipationData.mapperTo)))
    case GET -> Root / "organizations" as user =>
      Organizations.getByUser(user.typedId).foldM(errorResponse,
        p => Ok(p.map(OrgParticipationData.mapperTo)))
    case GET -> Root / "requests" as user =>
      OrganizationParticipants.getOwnRequests(user.typedId).foldM(errorResponse,
        r => Ok(r.map(OwnOrgParticipationRequest.mapperTo)))
  }

  def routes(implicit middleware: AuthMiddleware[Task, User]): HttpRoutes[Task] =
    Router("/user" -> httpRoutes)

  def docRoutes(basePath: String): List[Endpoint[_, _, _, _]] = {

    val authHeader = auth.bearer[String]()

    val userForm = jsonBody[UserForm]
      .description("The user data")
      .example(UserForm("User", "user@domain"))

    val getUser = endpoint.get
      .in(basePath / "user")
      .in(authHeader)
      .out(jsonBody[UserData])

    val putUser = endpoint.put
      .in(basePath / "user")
      .in(authHeader)
      .in(userForm)

    val deleteUser = endpoint.delete
      .in(basePath / "user")
      .in(authHeader)

    val postRevoke = endpoint.post
      .in(basePath / "user" / "revoke")
      .in(authHeader)

    val putSecret = endpoint.put
      .in(basePath / "user" / "secret")
      .in(authHeader)
      .in(jsonBody[SecretForm].description("New secret value"))
      .out(jsonBody[LoginData])

    val getEvents = endpoint.get
      .in(basePath / "user" / "events")
      .in(authHeader)
      .out(jsonBody[List[EventParticipationData]])

    val getOrganizations = endpoint.get
      .in(basePath / "user" / "organizations")
      .in(authHeader)
      .out(jsonBody[List[OrgParticipationData]])

    val getRequests = endpoint.get
      .in(basePath / "user" / "requests")
      .in(authHeader)
      .out(jsonBody[List[OwnOrgParticipationRequest]])

    List(getUser, putUser, deleteUser, postRevoke, putSecret, getEvents, getOrganizations, getRequests)
  }
}
