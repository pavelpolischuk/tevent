package tevent.notification

import tevent.organizations.dto.{OrgParticipationRequest => _}
import tevent.core.Config.GmailConfig
import Email.GmailSender
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertCompletes, _}
import zio.{ULayer, ZLayer}

object GmailTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("Gmail")(

    testM("send test email") {
      for {
        _ <- Email.sendMail(testReceiver, "Test email", "This is test email from TEvent.")
      } yield assertCompletes
    }

  ).provideSomeLayer(sender) @@ ignore

  private val testReceiver = "receiverEmail"
  private val config = GmailConfig("senderName", "appSecret")
  private val sender: ULayer[Email] = ZLayer.succeed(new GmailSender(config))
}
