package cromwell.pipeline.utils.auth

import java.util.UUID

import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils

object TestUserUtils {
  def getDummyUser(password: String = "-Pa$$w0rd-", passwordSalt: String = "salt"): User = {
    val uuid = UUID.randomUUID().toString
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    User(
      userId = UserId(uuid),
      email = s"JohnDoe-$uuid@cromwell.com",
      passwordHash = passwordHash,
      passwordSalt = passwordSalt,
      firstName = "FirstName",
      lastName = "LastName",
      profilePicture = None
    )
  }
}
