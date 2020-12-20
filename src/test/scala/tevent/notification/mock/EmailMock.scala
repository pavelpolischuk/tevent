package tevent.notification.mock

import tevent.notification.Email
import zio.test.mock
import zio.test.mock.{Expectation, Mock}
import zio.{Has, URLayer, ZLayer}

object EmailMock extends Mock[Email] {
  object SendMail extends Effect[(String, String, String), Throwable, Unit]

  private object empty extends Effect[Unit, Nothing, Unit]

  val Empty: Expectation[Email] = empty().optional

  override val compose: URLayer[Has[mock.Proxy], Email] = ZLayer.fromServiceM { proxy =>
    withRuntime.as {
      (receiver: String, subject: String, content: String) => proxy(SendMail, receiver, subject, content)
    }
  }
}
