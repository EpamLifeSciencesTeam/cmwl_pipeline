package cromwell.pipeline.datastorage.dto

import cats.data.{ NonEmptyChain, Validated }
import cromwell.pipeline.utils.validator.Wrapped
import play.api.libs.json.{ Format, Json, OFormat }
import slick.lifted.MappedTo

final case class User(
  userId: UUID,
  email: UserEmail,
  passwordHash: String,
  passwordSalt: String,
  firstName: Name,
  lastName: Name,
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

final class UserEmail private (override val unwrap: String) extends AnyVal with Wrapped[String]

object UserEmail extends Wrapped.Companion {
  type Type = String
  type Wrapper = UserEmail
  type Error = String
  implicit lazy val emailFormat: Format[UserEmail] = Json.valueFormat[UserEmail]
  override protected def create(value: String): UserEmail = new UserEmail(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches("^[^@]+@[^\\.]+\\..+$"),
    value,
    NonEmptyChain.one("Email should match the following pattern <text_1>@<text_2>.<text_3>")
  )
}

final class Name private (override val unwrap: String) extends AnyVal with Wrapped[String]

object Name extends Wrapped.Companion {
  type Type = String
  type Wrapper = Name
  type Error = String
  implicit lazy val nameFormat: Format[Name] = Json.valueFormat[Name]
  override protected def create(value: String): Name = new Name(value)
  override protected def validate(value: String): ValidationResult[String] =
    Validated.cond(
      value.matches("^[a-zA-Z]+$"),
      value,
      NonEmptyChain.one("Name can contain only latin letters.")
    )
}

final class UUID private (override val unwrap: String) extends AnyVal with Wrapped[String]

object UUID extends Wrapped.Companion {
  type Type = String
  type Wrapper = UUID
  type Error = String
  val pattern: String = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
  implicit val uuidFormat: Format[UUID] = Json.valueFormat[UUID]
  override protected def create(value: String): UUID = new UUID(value)
  override protected def validate(value: String): ValidationResult[String] = Validated.cond(
    value.matches(pattern),
    value,
    NonEmptyChain.one("Invalid UUID")
  )

  def random: UUID = new UUID(java.util.UUID.randomUUID().toString)
}
