package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped
import slick.lifted.MappedTo

final class ProjectConfigurationId private (override val unwrap: String)
    extends AnyVal
    with Wrapped[String]
    with MappedTo[String] {
  override def value: String = unwrap
}

object ProjectConfigurationId extends Wrapped.Companion {
  type Type = String
  type Wrapper = ProjectConfigurationId
  type Error = String

  val pattern: String = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"

  override protected def create(value: String): ProjectConfigurationId = new ProjectConfigurationId(value)

  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches(pattern),
    value,
    NonEmptyChain.one("Invalid ProjectConfigurationId")
  )
}
