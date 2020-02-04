package cromwell.pipeline.datastorage.dto

import java.util.UUID

import cats.data.{ NonEmptyChain, Validated }
import cats.implicits._
import cromwell.pipeline.utils.validator.Wrapped
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{ Format, Json, OFormat }
import slick.lifted.MappedTo

final case class User(
  userId: UserId,
  email: UserEmail,
  passwordHash: String,
  passwordSalt: String,
  firstName: FirstName,
  lastName: LastName,
  profilePicture: Option[ProfilePicture] = None,
  active: Boolean = true
)

object User {
  implicit lazy val userFormat: OFormat[User] = Json.format[User]
}

final case class ProfilePicture(value: Array[Byte]) extends MappedTo[Array[Byte]]

object ProfilePicture {
  implicit lazy val profilePictureFormat: OFormat[ProfilePicture] = Json.format[ProfilePicture]
}

final class UserId private (override val unwrap: UUID) extends AnyVal with Wrapped[UUID] with MappedTo[String] {
  override def value: String = unwrap.toString
}
object UserId extends Wrapped.Companion {
  type Type = UUID
  type Wrapper = UserId
  type Error = String
  implicit lazy val userIdFormat: Format[UserId] =
    implicitly[Format[String]].inmap((UUID.fromString _).andThen(UserId.apply), _.value)

  override protected def create(value: UUID): UserId = new UserId(value)
  override protected def validate(value: UUID): ValidationResult[UUID] = Validated.cond(
    value.toString.length == 36,
    value,
    NonEmptyChain.one("Invalid Id")
  )
}

final class UserEmail private (override val unwrap: String) extends AnyVal with Wrapped[String] with MappedTo[String] {
  override def value: String = unwrap
}
object UserEmail extends Wrapped.Companion {
  type Type = String
  type Wrapper = UserEmail
  type Error = String
  implicit lazy val emailFormat: Format[UserEmail] = implicitly[Format[String]].inmap(UserEmail.apply, _.value)
  override protected def create(value: String): UserEmail = new UserEmail(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches("^[^@]+@[^\\.]+\\..+$"),
    value,
    NonEmptyChain.one("Invalid email")
  )
}

final class FirstName private (override val unwrap: String) extends AnyVal with Wrapped[String] with MappedTo[String] {
  override def value: String = unwrap
}
object FirstName extends Wrapped.Companion {
  type Type = String
  type Wrapper = FirstName
  type Error = String
  implicit lazy val nameFormat: Format[FirstName] = implicitly[Format[String]].inmap(FirstName.apply, _.value)
  override protected def create(value: String): FirstName = new FirstName(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches("^[a-zA-Z]+$"),
    value,
    NonEmptyChain.one("Invalid first name")
  )
}

final class LastName private (override val unwrap: String) extends AnyVal with Wrapped[String] with MappedTo[String] {
  override def value: String = unwrap
}
object LastName extends Wrapped.Companion {
  type Type = String
  type Wrapper = LastName
  type Error = String
  implicit lazy val nameFormat: Format[LastName] = implicitly[Format[String]].inmap(LastName.apply, _.value)
  override protected def create(value: String): LastName = new LastName(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches("^[a-zA-Z]+$"),
    value,
    NonEmptyChain.one("Invalid last name")
  )
}
