package tevent.user

import io.circe.syntax.EncoderOps
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.http4s.{Method, Request, Status}
import tevent.core.ValidationError
import tevent.helpers.TestData._
import tevent.helpers.TestHelper.checkRequest
import tevent.user.dto.LoginData
import tevent.user.mock.AuthMock
import tevent.user.service.Auth
import zio.RIO
import zio.interop.catz.taskConcurrentInstance
import zio.test.Assertion.equalTo
import zio.test.{testM, _}
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation

object AuthEndpointTest extends DefaultRunnableSpec {
  type Task[A] = RIO[Auth, A]

  override def spec: ZSpec[TestEnvironment, Any] = suite("AuthEndpoint")(

    testM("should signin user") {
      val auth = AuthMock.SignIn(equalTo((userLogin.name.get, userLogin.email, userLogin.secret)), Expectation.value(userToken))
      val postReq = Request[Task](Method.POST, uri"/signin")
        .withEntity(userLogin.asJson)
      checkRequest(app.run(postReq), Status.Ok, Some(LoginData("Signed in", userToken.signedString)))
        .provideSomeLayer(auth)
    },
    testM("should fail signin existing email") {
      val auth = AuthMock.SignIn(equalTo((userLogin.name.get, userLogin.email, userLogin.secret)),
        Expectation.failure(ValidationError("existing")))
      val postReq = Request[Task](Method.POST, uri"/signin")
        .withEntity(userLogin.asJson)
      app.run(postReq).map(result => assert(result.status)(equalTo(Status.BadRequest)))
        .provideSomeLayer(auth)
    },
    testM("should login user") {
      val auth = AuthMock.Login(equalTo((userLogin.email, userLogin.secret)), Expectation.value(userToken))
      val postReq = Request[Task](Method.POST, uri"/login")
        .withEntity(userLogin.copy(name=None).asJson)
      checkRequest(app.run(postReq), Status.Ok, Some(LoginData("Logged in", userToken.signedString)))
        .provideSomeLayer(auth)
    },
    testM("should fail bad login") {
      val auth = AuthMock.SignIn(equalTo((userLogin.name.get, userLogin.email, userLogin.secret)),
        Expectation.failure(ValidationError("Bad secret")))
      val postReq = Request[Task](Method.POST, uri"/signin")
        .withEntity(userLogin.asJson)
      app.run(postReq).map(result => assert(result.status)(equalTo(Status.BadRequest)))
        .provideSomeLayer(auth)
    },
    testM("should auth with google") {
      val auth = AuthMock.Google(equalTo(googleToken.idtoken), Expectation.value(userToken))
      val postReq = Request[Task](Method.POST, uri"/auth/google")
        .withEntity(googleToken.asJson)
      checkRequest(app.run(postReq), Status.Ok, Some(LoginData("Logged in", userToken.signedString)))
        .provideSomeLayer(auth)
    }
  )

  private val endpoint = new AuthEndpoint[Auth]
  private val app = endpoint.routes.orNotFound
}
