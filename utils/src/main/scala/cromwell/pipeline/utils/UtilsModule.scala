package cromwell.pipeline.utils

import cromwell.pipeline.utils.auth.AuthUtils

class UtilsModule(applicationConfig: ApplicationConfig) {

  lazy val authUtils: AuthUtils = new AuthUtils(applicationConfig.authConfig)
}
