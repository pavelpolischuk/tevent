package tevent.user.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import tevent.core.Config.AuthConfig
import tevent.core.{Config, ExecutionError}
import tevent.user.model.GoogleToken
import zio.{IO, URLayer, ZIO}

import java.util.Collections

object GoogleAuth {
  trait Service {
    def getInfo(idToken: String): IO[ExecutionError, GoogleToken]
  }

  class GoogleAuthClient(clients: AuthConfig) extends Service {
    override def getInfo(idToken: String): IO[ExecutionError, GoogleToken] =
      ZIO.effect {
        val transport = GoogleNetHttpTransport.newTrustedTransport()
        val json = JacksonFactory.getDefaultInstance
        val verifier = new GoogleIdTokenVerifier.Builder(transport, json)
          .setAudience(Collections.singletonList(clients.google))
          .build

        verifier.verify(idToken)
      }
        .mapError(ExecutionError)
        .filterOrFail(_ != null)(ExecutionError(new NullPointerException("Google IdToken after verify is null")))
        .map { token =>
          val payload = token.getPayload
          //val emailVerified = Boolean.valueOf(payload.getEmailVerified)
          GoogleToken(payload.getSubject, payload.getEmail,
            payload.get("name").asInstanceOf[String],
            payload.get("picture").asInstanceOf[String],
            payload.get("family_name").asInstanceOf[String])
        }
  }

  def live: URLayer[Config, GoogleAuth] = ZIO.service[AuthConfig].map(c => new GoogleAuthClient(c)).toLayer

  def getInfo(idToken: String): ZIO[GoogleAuth, ExecutionError, GoogleToken] =
    ZIO.accessM(_.get.getInfo(idToken))
}
