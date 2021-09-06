package cromwell.pipeline.service.impls

import cromwell.pipeline.datastorage.dto.UserWithCredentials
import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, SignInRequest, SignUpRequest }
import cromwell.pipeline.service.AuthService

import scala.concurrent.Future

class AuthServiceTestImpl(authResponses: Seq[AuthResponse], testMode: TestMode) extends AuthService {

  override def signIn(request: SignInRequest): Future[Option[AuthResponse]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(authResponses.headOption)
    }

  override def signUp(request: SignUpRequest): Future[Option[AuthResponse]] =
    testMode match {
      case WithException(exc) => Future.failed(exc)
      case _                  => Future.successful(authResponses.headOption)
    }

  override def refreshTokens(refreshToken: String): Option[AuthResponse] = authResponses.headOption

  override def responseFromUser(user: UserWithCredentials): Option[AuthResponse] = authResponses.headOption

  override def passwordCorrect(request: SignInRequest, user: UserWithCredentials): Option[Throwable] =
    testMode match {
      case WithException(exc) => Some(exc)
      case _                  => None
    }

  override def userIsActive(user: UserWithCredentials): Option[Throwable] =
    testMode match {
      case WithException(exc) => Some(exc)
      case _                  => None
    }

}

object AuthServiceTestImpl {

  def apply(authResponses: AuthResponse*): AuthServiceTestImpl =
    new AuthServiceTestImpl(authResponses = authResponses, testMode = Success)

  def withException(exception: Throwable): AuthServiceTestImpl =
    new AuthServiceTestImpl(authResponses = Seq.empty, testMode = WithException(exception))

}
