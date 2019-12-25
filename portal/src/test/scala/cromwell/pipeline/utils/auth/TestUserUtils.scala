package cromwell.pipeline.utils.auth

import java.util.UUID

import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils

object TestUserUtils {
  val userPassword = "-Pa$$w0rd-"

  def getDummyUser(password: String = userPassword, passwordSalt: String = "salt", active: Boolean = true): User = {
    val uuid = UUID.randomUUID().toString
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    User(
      userId = UserId(uuid),
      email = s"JohnDoe-$uuid@cromwell.com",
      passwordHash = passwordHash,
      passwordSalt = passwordSalt,
      firstName = "FirstName",
      lastName = "LastName",
      profilePicture = None,
      active = active
    )
  }
}
