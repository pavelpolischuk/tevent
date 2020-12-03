package tevent.notification

import tevent.http.model.organization.{OrgParticipationRequest => _}
import tevent.infrastructure.Configuration.GmailConfig
import tevent.infrastructure.service.EmailSender.GmailSender
import tevent.infrastructure.service.{Email, EmailSender}
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertCompletes, _}
import zio.{ULayer, ZLayer}

object GmailTest extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("Gmail")(

    testM("send test email") {
      for {
        _ <- EmailSender.sendMail(testReceiver, "Test email", "This is test email from TEvent.")
      } yield assertCompletes
    }

  ).provideSomeLayer(sender) @@ ignore

  private val testReceiver = "receiverEmail"
  private val config = GmailConfig("senderName", "appSecret")
  private val sender: ULayer[Email] = ZLayer.succeed(new GmailSender(config))
}
