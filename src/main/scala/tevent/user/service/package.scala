package tevent.user

import zio.Has

package object service {
  type Crypto = Has[Crypto.Service]

  type Auth = Has[Auth.Service]
  type GoogleAuth = Has[GoogleAuth.Service]
  type Users = Has[Users.Service]
}
