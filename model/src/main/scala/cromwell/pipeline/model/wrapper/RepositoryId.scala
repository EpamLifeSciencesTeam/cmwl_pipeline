package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped
import slick.lifted.MappedTo

final case class RepositoryId(override val unwrap: Int) extends AnyVal with MappedTo[Int] with Wrapped[Int] {
  override def value: Int = unwrap
}

object RepositoryId extends Wrapped.Companion {
  type Type = Int
  type Wrapper = RepositoryId
  type Error = String

  override protected def create(value: Int): RepositoryId = new RepositoryId(value)
  override protected def validate(value: Int): ValidationResult[Int] = Validated.cond(
    value >= 0,
    value,
    NonEmptyChain.one("Value should be not negative integer")
  )
}
