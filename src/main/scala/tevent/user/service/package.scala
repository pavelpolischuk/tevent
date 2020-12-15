package tevent.user

import zio.Has

package object service {
  type Auth = Has[Auth.Service]
  type Crypto = Has[Crypto.Service]
  type Users = Has[Users.Service]
}
