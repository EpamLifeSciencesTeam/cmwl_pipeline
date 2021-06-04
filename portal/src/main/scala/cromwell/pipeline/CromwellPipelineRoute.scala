package cromwell.pipeline

import akka.http.scaladsl.model.headers.HttpOrigin
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.javadsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import cromwell.pipeline.controller.ControllerModule
import cromwell.pipeline.datastorage.dto.auth.AccessTokenContent
import cromwell.pipeline.utils.ApplicationConfig

object CromwellPipelineRoute {
  type SecuredRoute = AccessTokenContent => Route
  def routeCombiner(routes: SecuredRoute*): SecuredRoute = token => concat(routes.map(_(token)): _*)
}

final class CromwellPipelineRoute(applicationConfig: ApplicationConfig, controllerModule: ControllerModule) {
  import CromwellPipelineRoute._
  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
  import controllerModule._

  private val allowedOrigins = applicationConfig.webServiceConfig.cors.allowedOrigins.map(HttpOrigin(_))
  private val httpOriginMatcher = HttpOriginMatcher.create(allowedOrigins: _*)
  private val corsSettings = CorsSettings.defaultSettings.withAllowedOrigins(httpOriginMatcher)

  val route: Route = cors(corsSettings) {
    authController.route ~ securityDirective.authenticated {
      routeCombiner(
        userController.route,
        projectController.route,
        projectFileController.route,
        runController.route,
        configurationController.route
      )
    }
  }
}
