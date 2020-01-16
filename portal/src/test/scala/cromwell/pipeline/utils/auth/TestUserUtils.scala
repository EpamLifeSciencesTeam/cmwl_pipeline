package cromwell.pipeline.utils.auth

import java.util.UUID

import cromwell.pipeline.datastorage.dto.{ User, UserId }
import cromwell.pipeline.utils.StringUtils

object TestUserUtils {
  val userPassword = "-Pa$$w0rd-"

  def getDummyUser(password: String = userPassword, passwordSalt: String = "salt", active: Boolean = true): User = {
    val uuid = UUID.randomUUID().toString
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    createDummyUser(
      UserId(uuid),
      s"JohnDoe-$uuid@cromwell.com",
      passwordHash,
      passwordSalt,
      "FirstName",
      "LastName",
      active
    )
  }

  def getDummyUserWithCustomEmailDomain(
    password: String = userPassword,
    passwordSalt: String = "salt",
    active: Boolean = true,
    emailDomain: String
  ): User = {
    val uuid = UUID.randomUUID().toString
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    createDummyUser(
      UserId(uuid),
      s"JohnDoe-$uuid@$emailDomain",
      passwordHash,
      passwordSalt,
      "FirstName",
      "LastName",
      active
    )
  }

  def getDummyUserWithWrongEmailPattern(
    password: String = userPassword,
    passwordSalt: String = "salt",
    active: Boolean = true
  ): User = {
    val uuid = UUID.randomUUID().toString
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    createDummyUser(
      UserId(uuid),
      s"JohnDoe-$uuid-cromwell.com",
      passwordHash,
      passwordSalt,
      "FirstName",
      "LastName",
      active
    )
  }

  def createDummyUser(
    userId: UserId,
    email: String,
    passwordHash: String,
    passwordSalt: String,
    firstName: String,
    lastName: String,
    active: Boolean
  ): User =
    User(
      userId,
      email,
      passwordHash,
      passwordSalt,
      firstName,
      lastName,
      profilePicture = None,
      active = true
    )

}
