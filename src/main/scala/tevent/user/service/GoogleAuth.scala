package tevent.user.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import tevent.core.ExecutionError
import tevent.user.model.GoogleToken
import zio.{IO, ULayer, ZIO}

object GoogleAuth {
  trait Service {
    def getInfo(idToken: String): IO[ExecutionError, GoogleToken]
  }

  def live: ULayer[GoogleAuth] = ZIO.succeed(new Service {
    override def getInfo(idToken: String): IO[ExecutionError, GoogleToken] =
      ZIO.effect {
          val transport = GoogleNetHttpTransport.newTrustedTransport()
          val json = JacksonFactory.getDefaultInstance
          val verifier = new GoogleIdTokenVerifier.Builder(transport, json)
            //.setAudience(Collections.singletonList(CLIENT_ID))
            //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
            .build

          verifier.verify(idToken)
        }
        .mapError(ExecutionError)
        .filterOrFail(_ == null)(ExecutionError(new NullPointerException("Google IdToken after verify is null")))
        .map{ token =>
          val payload = token.getPayload
          //val emailVerified = Boolean.valueOf(payload.getEmailVerified)
          GoogleToken(payload.getSubject, payload.getEmail,
            payload.get("name").asInstanceOf[String],
            payload.get("picture").asInstanceOf[String],
            payload.get("family_name").asInstanceOf[String])
        }
  }).toLayer
}
