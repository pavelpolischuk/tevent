package tevent.user.mock

import tevent.core.ExecutionError
import tevent.user.model.GoogleToken
import tevent.user.service.GoogleAuth
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, IO, URLayer, ZLayer}

object GoogleAuthMock extends Mock[GoogleAuth] {
  object GetInfo extends Effect[String, ExecutionError, GoogleToken]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[GoogleAuth] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], GoogleAuth] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
        new GoogleAuth.Service {
          override def getInfo(idToken: String): IO[ExecutionError, GoogleToken] = proxy(GetInfo, idToken)
        }
    }
  }
}
