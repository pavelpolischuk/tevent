package tevent.organizations.dto

import tevent.organizations.model.OrganizationId

object OrganizationIdVar {
  def unapply(str: String): Option[OrganizationId] =
    if (str.nonEmpty)
      str.toLongOption.map(OrganizationId(_))
    else
      None
}
