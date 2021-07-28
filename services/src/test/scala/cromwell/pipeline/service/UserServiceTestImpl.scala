package cromwell.pipeline.service
import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserNoCredentials }
import cromwell.pipeline.model.wrapper.{ UserEmail, UserId }
import cromwell.pipeline.utils.StringUtils.calculatePasswordHash

import scala.collection.mutable
import scala.concurrent.Future

class UserServiceTestImpl extends UserService {

  val users: mutable.Map[UserId, User] = mutable.Map.empty

  private[service] def addDummies(dummies: User*): Unit =
    dummies.foreach(user => users += (user.userId -> user))
  private[service] def clearTestRepository(): Unit = users.clear()

  override def getUsersByEmail(emailPattern: String): Future[Seq[User]] =
    Future.successful(users.values.filter(_.email.unwrap.contains(emailPattern)).toSeq)

  override def getUserByEmail(email: UserEmail): Future[Option[User]] =
    Future.successful(users.values.find(_.email == email))

  override def addUser(user: User): Future[UserId] = {
    users += (user.userId -> user)
    Future.successful(user.userId)
  }

  override def deactivateUserById(userId: UserId): Future[Option[UserNoCredentials]] = {
    val optDeactivatedUser = users.get(userId).map { user =>
      val deactivatedUser = user.copy(active = false)
      users += (userId -> deactivatedUser)
      UserNoCredentials.fromUser(deactivatedUser)
    }
    Future.successful(optDeactivatedUser)
  }

  override def updateUser(userId: UserId, request: UserUpdateRequest): Future[Int] =
    users.get(userId) match {
      case Some(user) =>
        val updatedUser =
          user.copy(email = request.email, firstName = request.firstName, lastName = request.lastName)
        users += (userId -> updatedUser)
        Future.successful(1)
      case None => Future.failed(new RuntimeException("user with this id doesn't exist"))
    }

  override def updatePassword(userId: UserId, request: PasswordUpdateRequest, salt: String): Future[Int] =
    if (request.newPassword == request.repeatPassword) {
      users.get(userId) match {
        case Some(user) =>
          user match {
            case user
                if user.passwordHash == calculatePasswordHash(request.currentPassword.unwrap, user.passwordSalt) => {
              val passwordSalt = salt
              val passwordHash = calculatePasswordHash(request.newPassword.unwrap, passwordSalt)
              users += (userId -> user.copy(passwordSalt = passwordSalt, passwordHash = passwordHash))
              Future.successful(1)
            }
            case _ => Future.failed(new RuntimeException("user password differs from entered"))
          }
        case None => Future.failed(new RuntimeException("user with this id doesn't exist"))
      }
    } else Future.failed(new RuntimeException("new password incorrectly duplicated"))

}
object UserServiceTestImpl {
  def apply(testUsers: User*): UserServiceTestImpl = {
    val userServiceTestImpl = new UserServiceTestImpl
    testUsers.foreach(user => userServiceTestImpl.users += (user.userId -> user))
    userServiceTestImpl
  }
}
