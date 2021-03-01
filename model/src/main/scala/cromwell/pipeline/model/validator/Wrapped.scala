package cromwell.pipeline.model.validator

import cats.Show
import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }
import cats.data.{ NonEmptyChain, Validated }
import play.api.libs.json.{ Format, JsError, JsResult, JsSuccess, JsValue, Json }

import scala.util.control.NoStackTrace
import scala.language.implicitConversions

trait Wrapped[T] extends Any {
  def unwrap: T
  def canEqual(that: Any): Boolean = this.getClass.isInstance(that)
  override def equals(that: Any): Boolean = canEqual(that) && this.unwrap.equals(that.asInstanceOf[Wrapped[T]].unwrap)
  override def hashCode: Int = this.getClass.hashCode + unwrap.hashCode()
  override def toString: String = unwrap.toString
}
object Wrapped {
  trait Companion[Type, Error, Wrapper <: Wrapped[Type]] {
    type ValidationResult[A] = Validated[NonEmptyChain[Error], A]
    implicit def wrappedOrdering(implicit ord: Ordering[Type]): Ordering[Wrapper] = Ordering.by(_.unwrap)
    implicit def wrapperFormat(implicit show: Show[Error], format: Format[Type]): Format[Wrapper] =
      new Format[Wrapper] {
        override def reads(json: JsValue): JsResult[Wrapper] = from(
          json.as[Type]
        ) match {
          case Valid(value)    => JsSuccess(value)
          case Invalid(errors) => JsError(errors.toNonEmptyList.show)
        }
        override def writes(wrapped: Wrapper): JsValue = Json.toJson(wrapped.unwrap)
      }
    protected def create(value: Type): Wrapper
    protected def validate(value: Type): ValidationResult[Type]
    def from(value: Type): ValidationResult[Wrapper] =
      validate(value).map(create)
    final def apply(value: Type, evidence: Enable.Unsafe.type)(implicit show: Show[Error]): Wrapper =
      validate(value) match {
        case Valid(content)  => create(content)
        case Invalid(errors) => throw new Enable.UnsafeException(errors.toNonEmptyList)
      }
  }
}

object Enable {
  object Unsafe
  final class UnsafeException[T: Show](errors: NonEmptyList[T]) extends Throwable with NoStackTrace {
    override lazy val getMessage: String = errors.show
  }
}
