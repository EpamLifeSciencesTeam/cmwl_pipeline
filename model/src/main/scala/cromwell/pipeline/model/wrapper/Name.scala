package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped
import play.api.libs.json.Format
import cats.implicits.catsStdShowForString

final class Name private (override val unwrap: String) extends AnyVal with Wrapped[String]

object Name extends Wrapped.Companion {
  type Type = String
  type Wrapper = Name
  type Error = String
  implicit lazy val nameFormat: Format[Name] = wrapperFormat
  override protected def create(value: String): Name = new Name(value)
  override protected def validate(value: String): ValidationResult[String] =
    Validated.cond(
      value.matches("^[a-zA-Z]+$"),
      value,
      NonEmptyChain.one("Name can contain only latin letters")
    )
}
