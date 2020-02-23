package cromwell.pipeline.utils.auth

import cromwell.pipeline.datastorage.dto.{ Name, UUID, User, UserEmail }
import cromwell.pipeline.utils.StringUtils
import cats.implicits._
import cromwell.pipeline.datastorage.dto.auth.Password

object TestUserUtils {
  val userPassword: Password = Password("-Pa$$w0rd-")

  def getDummyUser(
    uuid: UUID = UUID.random,
    password: String = userPassword,
    passwordSalt: String = "salt",
    firstName: Name = Name("FirstName"),
    lastName: Name = Name("LastName"),
    active: Boolean = true
  ): User = {
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    User(
      uuid,
      UserEmail(s"JohnDoe-${uuid.unwrap}@cromwell.com"),
      passwordHash,
      passwordSalt,
      firstName,
      lastName,
      None,
      active
    )
  }
}
