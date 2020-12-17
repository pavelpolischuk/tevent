package tevent.events.dto

import org.http4s.QueryParamDecoder
import org.http4s.dsl.io.{OptionalQueryParamDecoderMatcher, OptionalValidatingQueryParamDecoderMatcher}

import java.time.ZonedDateTime

object EventFilters {

  private val separators = Array('+', ' ')
  private implicit val stringArrayQueryParamDecoder: QueryParamDecoder[Array[String]] =
    QueryParamDecoder[String].map(s => s.split(separators))

  implicit val dateTimeQueryParamDecoder: QueryParamDecoder[ZonedDateTime] =
    QueryParamDecoder[String].map(ZonedDateTime.parse)

  object OptionalFromDateQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[ZonedDateTime]("fromDate")
  object OptionalToDateQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[ZonedDateTime]("toDate")
  object OptionalOrganizationIdQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Long]("organization")
  object OptionalLocationQueryParamMatcher extends OptionalQueryParamDecoderMatcher[String]("location")
  object OptionalTagsQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Array[String]]("tags")
}
