package tevent.http.model.event

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.{OptionalQueryParamDecoderMatcher, OptionalValidatingQueryParamDecoderMatcher}

import java.time.ZonedDateTime

object EventFilters {

  implicit val dateTimeQueryParamDecoder: QueryParamDecoder[ZonedDateTime] =
    QueryParamDecoder[String].map(ZonedDateTime.parse)

  object OptionalFromDateQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[ZonedDateTime]("fromDate")
  object OptionalToDateQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[ZonedDateTime]("toDate")
  object OptionalOrganizationIdQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Long]("organization")
  object OptionalLocationQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("location")
}
