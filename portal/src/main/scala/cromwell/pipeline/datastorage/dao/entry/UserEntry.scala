package cromwell.pipeline.datastorage.dao.entry

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dto.{ProfilePicture, User, UserId}

class UserEntry(val pipelineDatabaseEngine: PipelineDatabaseEngine) {

  import pipelineDatabaseEngine.profile.api._

  class Users(tag: Tag) extends Table[User](tag, "user") {
    def userId = column[UserId]("user_id", O.PrimaryKey)
    def email = column[String]("email")
    def passwordHash = column[String]("password_hash")
    def passwordSalt = column[String]("password_salt")
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def profilePicture = column[ProfilePicture]("profile_picture")
    def * = (userId, email, passwordHash, passwordSalt, firstName, lastName, profilePicture.?) <>
      (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

  def getUserByIdAction = Compiled { userId: Rep[UserId] =>
    users.filter(_.userId === userId).take(1)
  }

  def getUserByEmailAction = Compiled { email: Rep[String] =>
    users.filter(_.email === email).take(1)
  }

  def addUserAction(user: User) = (users returning users.map(_.userId)) += user

}
