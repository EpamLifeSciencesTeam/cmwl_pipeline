package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped
import play.api.libs.json.Format
import cats.implicits.catsStdShowForString

final class UserId private (override val unwrap: String) extends AnyVal with Wrapped[String]

object UserId extends Wrapped.Companion {
  type Type = String
  type Wrapper = UserId
  type Error = String
  val pattern: String = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
  override protected def create(value: String): UserId = new UserId(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches(pattern),
    value,
    NonEmptyChain.one("Invalid UserId")
  )

  def random: UserId = new UserId(java.util.UUID.randomUUID().toString)
}
