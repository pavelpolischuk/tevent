package tevent.user

import tevent.core.ValidationError
import tevent.helpers.TestData._
import tevent.organizations.mock.OrganizationParticipantsMock
import tevent.user.mock._
import tevent.user.model.Email
import tevent.user.service.Auth
import zio.ZLayer
import zio.clock.Clock
import zio.duration.Duration
import zio.test.Assertion._
import zio.test._
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.mock.Expectation

object AuthServiceTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("Auth")(

    testM("login with secret") {
      val users = UsersMock.Find(equalTo(Email(user.email)), Expectation.value(user))
      val crypto = CryptoMock.VerifySecret(equalTo((userLogin.secret, user.secretHash.get)), Expectation.value(true)) ++
        CryptoMock.GetSignedToken(equalTo((user.id, userToken.issueTime)), Expectation.value(userToken))
      val service = ZLayer.identity[Clock] ++ (users ++ crypto ++ repo ++ orgs ++ google) >>> Auth.live
      for {
        _ <- TestClock.setTime(Duration.fromNanos(userToken.issueTime))
        token <- Auth.login(userLogin.email, userLogin.secret).provideLayer(service)
      } yield assert(token)(equalTo(userToken))
    },
    testM("fail login with bad secret") {
      val users = UsersMock.Find(equalTo(Email(user.email)), Expectation.value(user))
      val crypto = CryptoMock.VerifySecret(equalTo((userLogin.secret, user.secretHash.get)), Expectation.value(false))
      val service = ZLayer.identity[Clock] ++ (users ++ crypto ++ repo ++ orgs ++ google) >>> Auth.live
      val action = Auth.login(userLogin.email, userLogin.secret).provideLayer(service).run
      assertM(action)(fails(isSubtype[ValidationError](anything)))
    },
    testM("fail validate bad token") {
      val crypto = CryptoMock.ValidateSignedToken(equalTo(userToken.signedString), Expectation.failure(ValidationError("Invalid token")))
      val service = ZLayer.identity[Clock] ++ (crypto ++ users ++ repo ++ orgs ++ google) >>> Auth.live
      val action = Auth.validateUser(userToken.signedString).provideLayer(service).run
      assertM(action)(fails(isSubtype[ValidationError](anything)))
    },
    testM("fail validate old token") {
      val crypto = CryptoMock.ValidateSignedToken(equalTo(userToken.signedString), Expectation.value(userToken))
      val users = UsersMock.Get(equalTo(user.id), Expectation.value(user.copy(lastRevoke = userToken.issueTime + 1)))
      val service = ZLayer.identity[Clock] ++ (crypto ++ users ++ repo ++ orgs ++ google) >>> Auth.live
      val action = Auth.validateUser(userToken.signedString).provideLayer(service).run
      assertM(action)(fails(isSubtype[ValidationError](anything)))
    },

  )

  private val users = UsersMock.Empty
  private val repo = UsersRepositoryMock.Empty
  private val orgs = OrganizationParticipantsMock.Empty
  private val crypto = CryptoMock.Empty
  private val google = GoogleAuthMock.Empty
}
