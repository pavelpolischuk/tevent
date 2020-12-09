package tevent.domain.model

case class OrganizationFilter(tags: Array[String]) {
  def isEmpty: Boolean = tags.isEmpty
}

object OrganizationFilter {
  object All extends OrganizationFilter(Array.empty)
}
