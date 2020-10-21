package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped
import play.api.libs.json.Format
import cats.implicits.catsStdShowForString

final class RunId private (override val unwrap: String) extends AnyVal with Wrapped[String]

object RunId extends Wrapped.Companion {
  type Type = String
  type Wrapper = RunId
  type Error = String
  val pattern: String = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
  implicit lazy val runIdFormat: Format[RunId] = wrapperFormat
  override protected def create(value: String): RunId = new RunId(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches(pattern),
    value,
    NonEmptyChain.one("Invalid RunId")
  )

  def random: RunId = new RunId(java.util.UUID.randomUUID().toString)
}
