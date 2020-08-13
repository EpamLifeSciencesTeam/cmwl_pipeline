package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped

final class Name private (override val unwrap: String) extends AnyVal with Wrapped[String]

object Name extends Wrapped.Factory[String, String, Name] {
  override protected def create(value: String): Name = new Name(value)
  override protected def validate(value: String): ValidationResult[String] =
    Validated.cond(
      value.matches("^[a-zA-Z]+$"),
      value,
      NonEmptyChain.one("Name can contain only latin letters")
    )
}
