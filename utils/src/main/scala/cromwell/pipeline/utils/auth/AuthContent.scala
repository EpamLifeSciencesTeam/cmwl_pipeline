package cromwell.pipeline.utils.auth

import cromwell.pipeline.model.wrapper.UserId
import play.api.libs.json._

sealed trait AuthContent
object AuthContent {
  implicit val authContentFormat: OFormat[AuthContent] = Json.format[AuthContent]
}

final case class AccessTokenContent(userId: UserId) extends AuthContent
object AccessTokenContent {
  implicit val accessTokenContentFormat: OFormat[AccessTokenContent] = Json.format[AccessTokenContent]
}

final case class RefreshTokenContent private (userId: UserId, optRestOfUserSession: Option[Long]) extends AuthContent
object RefreshTokenContent {
  implicit val refreshTokenContentFormat: OFormat[RefreshTokenContent] = Json.format[RefreshTokenContent]
}
