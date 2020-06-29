package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cromwell.pipeline.datastorage.dto.auth.{ AuthResponse, SignInRequest, SignUpRequest }
import cromwell.pipeline.service.AuthService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

import scala.concurrent.ExecutionContext
import scala.util.Success

class AuthController(authService: AuthService)(implicit executionContext: ExecutionContext) {

  import AuthController._

  val route: Route = pathPrefix("auth") {
    concat(
      path("signIn") {
        post {
          entity(as[SignInRequest]) { request =>
            onComplete(authService.signIn(request)) {
              case Success(Some(authResponse)) => setSuccessAuthRoute(authResponse)
              case _                           => complete(StatusCodes.Unauthorized)
            }
          }
        }
      },
      path("signUp") {
        post {
          entity(as[SignUpRequest]) { request =>
            onComplete(authService.signUp(request)) {
              case Success(Some(authResponse)) => setSuccessAuthRoute(authResponse)
              case _                           => complete(StatusCodes.BadRequest)
            }
          }
        }
      },
      path("refresh") {
        get {
          parameter('refreshToken.as[String]) { refreshToken =>
            authService.refreshTokens(refreshToken) match {
              case Some(authResponse) => setSuccessAuthRoute(authResponse)
              case _                  => complete(StatusCodes.BadRequest)
            }
          }
        }
      }
    )
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
