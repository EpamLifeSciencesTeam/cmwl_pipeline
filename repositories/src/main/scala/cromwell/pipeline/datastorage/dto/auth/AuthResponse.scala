package cromwell.pipeline.datastorage.dto.auth

final case class AuthResponse(accessToken: String, refreshToken: String, accessTokenExpiration: Long)
