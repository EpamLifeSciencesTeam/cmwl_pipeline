package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped

final class ProjectSearchFilterId private (override val unwrap: String) extends AnyVal with Wrapped[String]

object ProjectSearchFilterId extends Wrapped.Companion {
  type Type = String
  type Wrapper = ProjectSearchFilterId
  type Error = String
  val pattern: String = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
  override protected def create(value: String): ProjectSearchFilterId = new ProjectSearchFilterId(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches(pattern),
    value,
    NonEmptyChain.one("Invalid ProjectSearchId")
  )

  def random: ProjectSearchFilterId = new ProjectSearchFilterId(java.util.UUID.randomUUID().toString)
}
