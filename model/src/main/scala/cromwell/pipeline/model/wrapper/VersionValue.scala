package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cats.implicits.catsStdShowForString
import cromwell.pipeline.model.validator.Wrapped
import play.api.libs.json.Format

final class VersionValue private (override val unwrap: Int) extends AnyVal with Wrapped[Int]

object VersionValue extends Wrapped.Companion {
  type Type = Int
  type Wrapper = VersionValue
  type Error = String
  val pattern = "^[0-9]+$"
  implicit val versionNumberFormat: Format[VersionValue] = wrapperFormat
  def increment(value: VersionValue): VersionValue = create(value.unwrap + 1)
  def fromString(value: String): ValidationResult[Wrapper] =
    validateString(value) match {
      case Validated.Valid(content)  => Validated.Valid(create(content.toInt))
      case Validated.Invalid(errors) => Validated.Invalid(errors)
    }
  protected def validateString(value: String): ValidationResult[String] = Validated.cond(
    value.matches(pattern),
    value,
    NonEmptyChain.one("Value should be not negative number")
  )
  override protected def create(value: Int): VersionValue = new VersionValue(value)
  override protected def validate(value: Int): ValidationResult[Int] = Validated.cond(
    value >= 0,
    value,
    NonEmptyChain.one("Value should be not negative")
  )
}
