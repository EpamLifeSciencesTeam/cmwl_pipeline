package cromwell.pipeline.datastorage.dao.entry

import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dto.{ ProfilePicture, User, UserId }

trait UserEntry {
  this: Profile =>

  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "user") {
    def userId = column[UserId]("user_id", O.PrimaryKey)
    def email = column[String]("email")
    def passwordHash = column[String]("password_hash")
    def passwordSalt = column[String]("password_salt")
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def profilePicture = column[ProfilePicture]("profile_picture")
    def active = column[Boolean]("active")
    def * = (userId, email, passwordHash, passwordSalt, firstName, lastName, profilePicture.?, active) <>
      (User.tupled, User.unapply)
  }

  val users = TableQuery[UserTable]

  def getUserByIdAction = Compiled { userId: Rep[UserId] =>
    users.filter(_.userId === userId).take(1)
  }

  def getUserByEmailAction = Compiled { email: Rep[String] =>
    users.filter(_.email === email).take(1)
  }

  def addUserAction(user: User) = (users.returning(users.map(_.userId))) += user

  def deactivateUserByEmail(email: String) = users.filter(_.email === email).map(_.active).update(false)

  def deactivateUserById(userId: UserId) = users.filter(_.userId === userId).map(_.active).update(false)

}
