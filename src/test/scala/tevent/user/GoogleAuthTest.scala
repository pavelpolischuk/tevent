package tevent.user

import tevent.core.Config.AuthConfig
import tevent.user.service.GoogleAuth
import tevent.user.service.GoogleAuth.GoogleAuthClient
import zio.test.TestAspect.ignore
import zio.test._
import zio.console
import zio.test.environment.TestEnvironment
import zio.{ULayer, ZLayer}

object GoogleAuthTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("GoogleAuth")(

    testM("check test token") {
      for {
        token <- GoogleAuth.getInfo(idToken)
        _ <- console.putStr(token.toString)
      } yield assertCompletes
    }

  ).provideCustomLayer(auth) @@ ignore

  private val idToken = "idToken"
  private val config = AuthConfig("googleClientId")
  private val auth: ULayer[GoogleAuth] = ZLayer.succeed(new GoogleAuthClient(config))
}
