package cromwell.pipeline.model.wrapper

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.model.validator.Wrapped
import slick.lifted.MappedTo

final class Password private (override val unwrap: String) extends AnyVal with Wrapped[String] with MappedTo[String] {
  override def value: String = unwrap
}

object Password extends Wrapped.Factory[String, String, Password] {
  override protected def create(value: String): Password = new Password(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches("(?=^.{10,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$"),
    value,
    NonEmptyChain.one(
      "Password must be at least 10 characters long, " +
        "including an uppercase and a lowercase letter, " +
        "one number and one special character."
    )
  )
}
