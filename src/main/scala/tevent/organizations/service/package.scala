package tevent.organizations

import zio.Has

package object service {
  type Organizations = Has[Organizations.Service]
  type OrganizationParticipants = Has[OrganizationParticipants.Service]
}
