package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.model.wrapper.{ Name, Password, UserEmail }

final case class AuthResponse(accessToken: String, refreshToken: String, accessTokenExpiration: Long)
final case class SignInRequest(email: UserEmail, password: Password)
final case class SignUpRequest(email: UserEmail, password: Password, firstName: Name, lastName: Name)
