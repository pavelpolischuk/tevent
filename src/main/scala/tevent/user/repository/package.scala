package tevent.user

import zio.Has

package object repository {
  type UsersT = Has[UsersTable]
  type UsersRepository = Has[UsersRepository.Service]
}
