package cromwell.pipeline.datastorage.utils.auth

import cromwell.pipeline.model.wrapper.UserId

sealed trait AuthContent
final case class AccessTokenContent(userId: UserId) extends AuthContent
final case class RefreshTokenContent private (userId: UserId, optRestOfUserSession: Option[Long]) extends AuthContent
