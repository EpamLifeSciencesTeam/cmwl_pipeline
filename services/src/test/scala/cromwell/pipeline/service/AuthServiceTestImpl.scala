package cromwell.pipeline.service
import cats.data.OptionT
import cromwell.pipeline.datastorage.dto.User
import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, SignInRequest, SignUpRequest }

import scala.concurrent.Future

class AuthServiceTestImpl(valToReturn: DummyToReturn) extends AuthService {

  lazy val wrongReturnedTypeException = new Exception("Wrong returned test value type")

  override def signIn(request: SignInRequest): Future[Option[AuthResponse]] =
    valToReturn match {
      case WithException(exc)                 => Future.failed(exc)
      case AuthResponseToReturn(authResponse) => Future.successful(Some(authResponse))
      case NoneToReturn                       => Future.successful(None)
      case _                                  => throw wrongReturnedTypeException
    }

  override def signUp(request: SignUpRequest): Future[Option[AuthResponse]] =
    valToReturn match {
      case WithException(exc)                 => Future.failed(exc)
      case AuthResponseToReturn(authResponse) => Future.successful(Some(authResponse))
      case _                                  => throw wrongReturnedTypeException
    }

  override def refreshTokens(refreshToken: String): Option[AuthResponse] =
    valToReturn match {
      case AuthResponseToReturn(authResponse) => Some(authResponse)
      case NoneToReturn                       => None
      case _                                  => throw wrongReturnedTypeException
    }

  override def takeUserFromRequest(request: SignInRequest): OptionT[Future, User] = ???

  override def responseFromUser(user: User): Option[AuthResponse] =
    valToReturn match {
      case AuthResponseToReturn(authResponse) => Some(authResponse)
      case NoneToReturn                       => None
      case _                                  => throw wrongReturnedTypeException
    }

  override def passwordCorrect(request: SignInRequest, user: User): Option[Throwable] =
    valToReturn match {
      case WithException(exc) => Some(exc)
      case _                  => None
    }

  override def userIsActive(user: User): Option[Throwable] =
    valToReturn match {
      case WithException(exc) => Some(exc)
      case _                  => None
    }

}

object AuthServiceTestImpl {
  def apply(valToReturn: DummyToReturn): AuthServiceTestImpl =
    new AuthServiceTestImpl(valToReturn)

}
