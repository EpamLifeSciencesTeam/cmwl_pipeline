package cromwell.pipeline.datastorage.dao.entry

import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ Name, UserEmail, UserId }
import slick.lifted.ProvenShape

trait UserEntry {
  this: Profile with AliasesSupport =>

  import Implicits._
  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, "user") {
    def userId: Rep[UserId] = column[UserId]("user_id", O.PrimaryKey)
    def email: Rep[UserEmail] = column[UserEmail]("email")
    def passwordHash: Rep[String] = column[String]("password_hash")
    def passwordSalt: Rep[String] = column[String]("password_salt")
    def firstName: Rep[Name] = column[Name]("first_name")
    def lastName: Rep[Name] = column[Name]("last_name")
    def profilePicture: Rep[ProfilePicture] = column[ProfilePicture]("profile_picture")
    def active: Rep[Boolean] = column[Boolean]("active")
    def * : ProvenShape[User] =
      (userId, email, passwordHash, passwordSalt, firstName, lastName, profilePicture.?, active) <>
        ((User.apply _).tupled, User.unapply)
  }

  val users = TableQuery[UserTable]

  def getUserByIdAction = Compiled { userId: Rep[UserId] =>
    users.filter(_.userId === userId).take(1)
  }

  def getUserByEmailAction = Compiled { email: Rep[UserEmail] =>
    users.filter(_.email === email).take(1)
  }

  def getUsersByEmailAction(emailPattern: String) =
    users.filter(_.email.like(emailPattern)).result

  def addUserAction(user: User): ActionResult[UserId] = users.returning(users.map(_.userId)) += user

  def deactivateUserByEmail(email: UserEmail): ActionResult[Int] =
    users.filter(_.email === email).map(_.active).update(false)

  def deactivateUserById(userId: UserId): ActionResult[Int] =
    users.filter(_.userId === userId).map(_.active).update(false)

  def updateUser(updatedUser: User): ActionResult[Int] =
    users
      .filter(_.userId === updatedUser.userId)
      .map(user => (user.email, user.firstName, user.lastName))
      .update((updatedUser.email, updatedUser.firstName, updatedUser.lastName))

  def updatePassword(userId: UserId, passwordHash: String, passwordSalt: String): ActionResult[Int] =
    users
      .filter(_.userId === userId)
      .map(user => (user.passwordHash, user.passwordSalt))
      .update((passwordHash, passwordSalt))
}
