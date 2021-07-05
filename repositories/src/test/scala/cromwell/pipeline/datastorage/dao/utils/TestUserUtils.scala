package cromwell.pipeline.datastorage.dao.utils

import cromwell.pipeline.datastorage.dto.UserWithCredentials
import cromwell.pipeline.utils.StringUtils
import cats.implicits._
import cromwell.pipeline.model.validator.Enable
import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail, UserId }

object TestUserUtils {
  val userPassword: Password = Password("-Pa$$w0rd1-", Enable.Unsafe)
  def getDummyUserId = UserId.random
  def getDummyUser(
    uuid: UserId = UserId.random,
    password: String = userPassword.unwrap,
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
}
