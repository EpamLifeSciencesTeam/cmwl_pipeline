package cromwell.pipeline.datastorage.dao.utils

import cromwell.pipeline.datastorage.dto.{ User, UserWithCredentials }
import cromwell.pipeline.utils.StringUtils
import cats.implicits._
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId }

object TestUserUtils {
  val userPassword: Password = Password("-Pa$$w0rd1-", Enable.Unsafe)
  def getDummyUserId: UserId = UserId.random
  def getDummyUserWithCredentials(
    uuid: UserId = UserId.random,
    password: Password = userPassword,
    passwordSalt: String = "salt",
    firstName: Name = Name("FirstName", Enable.Unsafe),
    lastName: Name = Name("LastName", Enable.Unsafe),
    active: Boolean = true
  ): UserWithCredentials = {
    val passwordHash = StringUtils.calculatePasswordHash(password, passwordSalt)
    UserWithCredentials(
      uuid,
      UserEmail(s"JohnDoe-$uuid@cromwell.com", Enable.Unsafe),
      passwordHash,
      passwordSalt,
      firstName,
      lastName,
      None,
      active
    )
  }

  def getDummyUser(
    uuid: UserId = UserId.random,
    firstName: Name = Name("FirstName", Enable.Unsafe),
    lastName: Name = Name("LastName", Enable.Unsafe),
    active: Boolean = true
  ): User = User.fromUserWithCredentials(
    getDummyUserWithCredentials(uuid = uuid, firstName = firstName, lastName = lastName, active = active)
  )

}
