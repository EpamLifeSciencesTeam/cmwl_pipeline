package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto.user.{ PasswordUpdateRequest, UserUpdateRequest }
import cromwell.pipeline.datastorage.dto.{ User, UserWithCredentials }
import cromwell.pipeline.model.wrapper.{ UserEmail, UserId }
import cromwell.pipeline.service.UserService

import scala.concurrent.Future

class UserServiceTestImpl(users: Seq[UserWithCredentials], testMode: TestMode) extends UserService {

  override def getUsersByEmail(emailPattern: String): Future[Seq[User]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(users.map(user => User.fromUserWithCredentials(user)))
    }

  override def getUserWithCredentialsByEmail(email: UserEmail): Future[Option[UserWithCredentials]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(users.headOption)
    }

  override def addUser(user: UserWithCredentials): Future[UserId] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(user.userId)
    }

  override def deactivateUserById(userId: UserId): Future[User] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(User.fromUserWithCredentials(users.head))
    }

  override def updateUser(userId: UserId, request: UserUpdateRequest): Future[Int] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(1)
    }

  override def updatePassword(userId: UserId, request: PasswordUpdateRequest, salt: String): Future[Int] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(1)
    }

}

object UserServiceTestImpl {

  def apply(users: UserWithCredentials*): UserServiceTestImpl =
    new UserServiceTestImpl(users = users, testMode = Success)

  def withException(exception: Throwable): UserServiceTestImpl =
    new UserServiceTestImpl(users = Seq.empty, testMode = WithException(exception))

}
