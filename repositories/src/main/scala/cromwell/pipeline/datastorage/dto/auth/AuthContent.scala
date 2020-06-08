package cromwell.pipeline.datastorage.dto.auth

import cromwell.pipeline.model.wrapper.UserId

sealed trait AuthContent
final case class AccessTokenContent(userId: UserId) extends AuthContent
final case class RefreshTokenContent private (userId: UserId, optRestOfUserSession: Option[Long]) extends AuthContent
