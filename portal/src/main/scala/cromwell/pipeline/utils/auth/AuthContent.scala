package cromwell.pipeline.utils.auth

import play.api.libs.json._

sealed trait AuthContent
object AuthContent {
  implicit val authContentFormat: OFormat[AuthContent] = Json.format[AuthContent]
}

final case class AccessTokenContent(userId: String) extends AuthContent
object AccessTokenContent {
  implicit val accessTokenContentFormat: OFormat[AccessTokenContent] = Json.format[AccessTokenContent]
}

final case class RefreshTokenContent private (userId: String, optRestOfUserSession: Option[Long]) extends AuthContent
object RefreshTokenContent {
  implicit val refreshTokenContentFormat: OFormat[RefreshTokenContent] = Json.format[RefreshTokenContent]
}
