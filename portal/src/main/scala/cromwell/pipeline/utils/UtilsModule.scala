package cromwell.pipeline.utils

import com.softwaremill.macwire._
import cromwell.pipeline.AuthConfig
import cromwell.pipeline.utils.auth.{AuthUtils, SecurityDirective}

@Module
class UtilsModule(authConfig: AuthConfig) {
  lazy val authUtils: AuthUtils = wire[AuthUtils]
  lazy val securityDirective: SecurityDirective = wire[SecurityDirective]
}
