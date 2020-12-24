package tevent

import tevent.core.{Config, Db}
import tevent.user.repository.{SlickUsersRepository, UsersRepository, UsersT}
import tevent.user.service.{Auth, Crypto, GoogleAuth, Users}
import zio.{URLayer, ZLayer}
import zio.clock.Clock

package object user {
  type Tables = UsersT
  type Repositories = UsersRepository
  type Services = Auth with GoogleAuth with Users

  val repositories: URLayer[Db with Tables, Repositories] =
    SlickUsersRepository.live
  val services: URLayer[Clock with Config with Crypto with Repositories, Services] =
    (ZLayer.identity[Clock with Config with Crypto with Repositories] >+> GoogleAuth.live >+> Users.live >+> Auth.live)
}
