package cromwell.pipeline.utils.auth

import java.util.UUID

import cromwell.pipeline.datastorage.dto.User.UserEmail
import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils

object TestUserUtils {
  val userPassword = "-Pa$$w0rd-"

  def getDummyUser(
    uuid: String = UUID.randomUUID().toString,
    emailOpt: UserEmail = "JohnDoe-@cromwell.com",
    password: String = userPassword,
    passwordSalt: String = "salt",
    firstName: String = "FirstName",
    active: Boolean = true
  ): User = {
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    User(
      UserId(uuid),
      s"JohnDoe-$uuid@cromwell.com",
      passwordHash,
      passwordSalt,
      "FirstName",
      "LastName",
      None,
      active
    )
  }
}
