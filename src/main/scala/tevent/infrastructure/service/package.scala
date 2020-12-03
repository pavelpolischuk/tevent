package tevent.infrastructure

import zio.Has

package object service {
  type Crypto = Has[Crypto.Service]
  type Email = Has[EmailSender.Service]
}
