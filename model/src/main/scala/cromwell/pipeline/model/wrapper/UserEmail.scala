package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped
import play.api.libs.json.Format
import cats.implicits.catsStdShowForString

final class UserEmail private (override val unwrap: String) extends AnyVal with Wrapped[String]

object UserEmail extends Wrapped.Companion {
  type Type = String
  type Wrapper = UserEmail
  type Error = String
  override protected def create(value: String): UserEmail = new UserEmail(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches(
      "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
    ),
    value,
    NonEmptyChain.one(
      "Email should match the following pattern <text>@<text>.<domain>," +
        "where <text> can contain: latin letters, numbers or underscore," +
        "and <domain> at least two characters and can contain only latin letters."
    )
  )
}
