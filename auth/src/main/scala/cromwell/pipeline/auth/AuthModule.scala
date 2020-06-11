package cromwell.pipeline.auth

import cromwell.pipeline.utils.ApplicationConfig

class AuthModule(applicationConfig: ApplicationConfig) {

  lazy val authUtils: AuthUtils = new AuthUtils(applicationConfig.authConfig)
  lazy val securityDirective: SecurityDirective = new SecurityDirective(applicationConfig.authConfig)
}
