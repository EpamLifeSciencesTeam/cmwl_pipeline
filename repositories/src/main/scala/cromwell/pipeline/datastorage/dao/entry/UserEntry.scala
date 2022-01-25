package cromwell.pipeline.datastorage.dao.entry

import cromwell.pipeline.datastorage.Profile
import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.model.wrapper.{ Name, UserEmail, UserId }
import slick.lifted.ProvenShape

trait UserEntry {
  this: Profile with AliasesSupport =>

  import Implicits._
  import profile.api._

  class UserTable(tag: Tag) extends Table[UserWithCredentials](tag, "user") {
    def userId: Rep[UserId] = column[UserId]("user_id", O.PrimaryKey)
    def email: Rep[UserEmail] = column[UserEmail]("email")
    def passwordHash: Rep[String] = column[String]("password_hash")
    def passwordSalt: Rep[String] = column[String]("password_salt")
    def firstName: Rep[Name] = column[Name]("first_name")
    def lastName: Rep[Name] = column[Name]("last_name")
    def profilePicture: Rep[ProfilePicture] = column[ProfilePicture]("profile_picture")
    def active: Rep[Boolean] = column[Boolean]("active")
    // scalastyle:off method.name
    def * : ProvenShape[UserWithCredentials] =
      (userId, email, passwordHash, passwordSalt, firstName, lastName, profilePicture.?, active) <>
        ((UserWithCredentials.apply _).tupled, UserWithCredentials.unapply)
    // scalastyle:on method.name
  }

  val users = TableQuery[UserTable]

  // scalastyle:off public.methods.have.type
  def getUserByIdAction = Compiled { userId: Rep[UserId] =>
    users.filter(_.userId === userId).take(1)
  }

  def getUserByEmailAction = Compiled { email: Rep[UserEmail] =>
    users.filter(_.email === email).take(1)
  }

  def getUsersByEmailAction(emailPattern: String) =
    users.filter(_.email.like(emailPattern)).result
  // scalastyle:on public.methods.have.type

  def addUserAction(user: UserWithCredentials): ActionResult[UserId] = users.returning(users.map(_.userId)) += user

  def deactivateUserByEmail(email: UserEmail): ActionResult[Int] =
    users.filter(_.email === email).map(_.active).update(false)

  def deactivateUserById(userId: UserId): ActionResult[Int] =
    users.filter(_.userId === userId).map(_.active).update(false)

  def updateUser(updatedUser: UserWithCredentials): ActionResult[Int] =
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
