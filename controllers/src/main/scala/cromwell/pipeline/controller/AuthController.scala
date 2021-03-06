package cromwell.pipeline.controller

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, SignInRequest, SignUpRequest }
import cromwell.pipeline.service.AuthService
import cromwell.pipeline.service.AuthorizationException.{
  DuplicateUserException,
  InactiveUserException,
  IncorrectPasswordException
}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.util.{ Failure, Success }

class AuthController(authService: AuthService) {

  import AuthController._

  private val signIn: Route = path("signIn") {
    post {
      entity(as[SignInRequest]) { request =>
        onComplete(authService.signIn(request)) {
          case Success(Some(authResponse)) => setSuccessAuthRoute(authResponse)
          case Failure(IncorrectPasswordException(message)) =>
            complete(HttpResponse(StatusCodes.Unauthorized, entity = message))
          case Failure(InactiveUserException(message)) =>
            complete(HttpResponse(StatusCodes.Forbidden, entity = message))
          case _ => complete(StatusCodes.Unauthorized)
        }
      }
    }
  }

  private val signUp: Route = path("signUp") {
    post {
      entity(as[SignUpRequest]) { request =>
        onComplete(authService.signUp(request)) {
          case Success(Some(authResponse)) => setSuccessAuthRoute(authResponse)
          case Failure(DuplicateUserException(message)) =>
            complete(HttpResponse(StatusCodes.BadRequest, entity = message))
          case _ => complete(StatusCodes.BadRequest)
        }
      }
    }
  }

  private val refreshToken: Route = path("refresh") {
    get {
      parameter('refreshToken.as[String]) { refreshToken =>
        authService.refreshTokens(refreshToken) match {
          case Some(authResponse) => setSuccessAuthRoute(authResponse)
          case _                  => complete(StatusCodes.BadRequest)
        }
      }
    }
  }

  val route: Route = pathPrefix("auth") {
    signIn ~
    signUp ~
    refreshToken
  }

  private def setSuccessAuthRoute(authResponse: AuthResponse): Route =
    respondWithHeaders(
      RawHeader(AccessTokenHeader, authResponse.accessToken),
      RawHeader(RefreshTokenHeader, authResponse.refreshToken),
      RawHeader(AccessTokenExpirationHeader, authResponse.accessTokenExpiration.toString)
    )(complete(StatusCodes.OK))
}

object AuthController {
  val AccessTokenHeader = "Access-Token"
  val RefreshTokenHeader = "Refresh-Token"
  val AccessTokenExpirationHeader = "Access-Token-Expiration"
}
