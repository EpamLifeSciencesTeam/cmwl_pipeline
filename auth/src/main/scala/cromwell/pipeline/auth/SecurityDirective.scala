package cromwell.pipeline.auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{ optionalHeaderValueByName, provide }
import akka.http.scaladsl.server.directives.RouteDirectives
import cromwell.pipeline.auth.token.MissingAccessTokenRejection
import cromwell.pipeline.datastorage.dto.auth.{ AccessTokenContent, AuthContent }
import cromwell.pipeline.utils.AuthConfig
import pdi.jwt.{ Jwt, JwtClaim }
import play.api.libs.json.Json
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import cromwell.pipeline.datastorage.formatters.AuthFormatters._

class SecurityDirective(authConfig: AuthConfig) {

  import RouteDirectives._
  import SecurityDirective._
  import authConfig._

  def authenticated: Directive1[AccessTokenContent] =
    optionalHeaderValueByName(AuthorizationHeader).flatMap {
      case Some(jwt) if !Jwt.isValid(jwt, secretKey, Seq(hmacAlgorithm)) =>
        complete(StatusCodes.Unauthorized -> UnauthorizedMessages.InvalidToken)
      case Some(jwt) =>
        getClaims(jwt)
          .map(claims => Json.parse(claims.content).as[AuthContent])
          .collect { case accessTokenContent: AccessTokenContent => accessTokenContent }
          .map(provide)
          .getOrElse(complete(StatusCodes.Unauthorized -> UnauthorizedMessages.AnotherTypeOfToken))
      case None => reject(MissingAccessTokenRejection(UnauthorizedMessages.MissedToken))
    }
  private def getClaims(jwt: String): Option[JwtClaim] = Jwt.decode(jwt, secretKey, Seq(hmacAlgorithm)).toOption

}

object SecurityDirective {
  val AuthorizationHeader = "Authorization"
  object UnauthorizedMessages {
    val MissedToken = "Missed access token."
    val InvalidToken = "Invalid access token."
    val AnotherTypeOfToken = "Another type of token."
  }
}
