package cromwell.pipeline.datastorage

import play.api.libs.json.{ Format, JsError, JsResult, JsSuccess, JsValue, Json }
import cats.Show
import cromwell.pipeline.model.validator.Wrapped
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId, VersionValue }
import scala.language.implicitConversions
import cats.data.Validated.{ Invalid, Valid }
import cats.implicits.catsStdShowForString
import cromwell.pipeline.model.validator.Wrapped.Factory

package object formatters {
  private def wrapperFormat[T, E, W <: Wrapped[T]](wrapperFactory: Factory[T, E, W])(
    implicit show: Show[E],
    format: Format[T]
  ): Format[W] =
    new Format[W] {
      override def reads(json: JsValue): JsResult[W] = json
        .validate[T]
        .flatMap[W](
          value =>
            wrapperFactory.from(value) match {
              case Valid(wrapped) => JsSuccess(wrapped)
              case Invalid(e)     => JsError(s"Microtype is not valid: $e")
            }
        )
      override def writes(wrapped: W): JsValue = Json.toJson(wrapped.unwrap)
    }

  implicit lazy val nameFormat: Format[Name] = wrapperFormat(Name)
  implicit lazy val passwordFormat: Format[Password] = wrapperFormat(Password)
  implicit lazy val userEmailFormat: Format[UserEmail] = wrapperFormat(UserEmail)
  implicit lazy val userIdFormat: Format[UserId] = wrapperFormat(UserId)
  implicit lazy val versionValueFormat: Format[VersionValue] = wrapperFormat(VersionValue)
}
