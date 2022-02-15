package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped
import slick.lifted.MappedTo

import java.util.UUID.randomUUID

final class ProjectId private (override val unwrap: String) extends AnyVal with Wrapped[String] with MappedTo[String] {
  override def value: String = unwrap
}

object ProjectId extends Wrapped.Companion {
  type Type = String
  type Wrapper = ProjectId
  type Error = String

  val pattern: String = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"

  override protected def create(value: String): ProjectId = new ProjectId(value)

  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches(pattern),
    value,
    NonEmptyChain.one("Invalid ProjectId")
  )

  def random: ProjectId = new ProjectId(randomUUID().toString)
}
