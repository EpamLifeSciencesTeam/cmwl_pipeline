package cromwell.pipeline.auth

import cromwell.pipeline.utils.AuthConfig

class AuthModule(authConfig: AuthConfig) {

  lazy val authUtils: AuthUtils = AuthUtils(authConfig)
  lazy val securityDirective: SecurityDirective = new SecurityDirective(authConfig)
}
