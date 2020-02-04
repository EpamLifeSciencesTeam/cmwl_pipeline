package cromwell.pipeline.utils.validator

import cats.Show
import cats.data.{ NonEmptyChain, NonEmptyList, Validated }
import cats.data.Validated.{ Invalid, Valid }
import scala.util.control.NoStackTrace
import scala.language.implicitConversions

trait Wrapped[T] extends Any {
  def unwrap: T
  def canEqual(that: Any): Boolean = this.getClass.isInstance(that)
  override def equals(that: Any): Boolean = canEqual(that) && this.unwrap.equals(that.asInstanceOf[Wrapped[T]].unwrap)
  override def hashCode: Int = this.getClass.hashCode + unwrap.hashCode()
  override def toString: String = s"${this.getClass.getSimpleName}(${unwrap.toString})"
}
object Wrapped {
  trait Companion {
    type Type
    type Error
    type Wrapper <: Wrapped[Type]
    type ValidationResult[A] = Validated[NonEmptyChain[Error], A]
    implicit def wrappedOrdering(implicit ord: Ordering[Type]): Ordering[Wrapper] = Ordering.by(_.unwrap)
    implicit def unwrap(wrapped: Wrapper): Type = wrapped.unwrap
    protected def create(value: Type): Wrapper
    protected def validate(value: Type): ValidationResult[Type]
    def from(value: Type): ValidationResult[Wrapper] =
      validate(value) match {
        case Valid(x)        => Valid(create(x))
        case Invalid(errors) => Invalid(errors)
      }
    final def apply(value: Type)(implicit evidence: Enable.Unsafe.type, show: Show[Error]): Wrapper =
      validate(value) match {
        case Valid(x)        => create(x)
        case Invalid(errors) => throw new Enable.UnsafeException(errors.toNonEmptyList)
      }
  }
}
object Enable {
  implicit object Unsafe
  final class UnsafeException[T: Show](errors: NonEmptyList[T]) extends Throwable with NoStackTrace {
    override lazy val getMessage: String = errors.show
  }
}
