package tevent.http.model.organization

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.OptionalValidatingQueryParamDecoderMatcher

object OrganizationFilters {

  private val separators = Array('+', ' ')

  implicit val stringArrayQueryParamDecoder: QueryParamDecoder[Array[String]] =
    QueryParamDecoder[String].map(s => s.split(separators))

  object OptionalTagsQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Array[String]]("tags")
}
