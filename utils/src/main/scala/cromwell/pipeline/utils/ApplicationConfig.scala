package cromwell.pipeline.utils

import com.typesafe.config.{ Config, ConfigFactory }
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm

sealed trait ConfigComponent

final case class WebServiceConfig(interface: String, port: Int) extends ConfigComponent

final case class AuthConfig(
  secretKey: String,
  hmacAlgorithm: JwtHmacAlgorithm,
  expirationTimeInSeconds: ExpirationTimeInSeconds
) extends ConfigComponent

final case class ExpirationTimeInSeconds(accessToken: Long, refreshToken: Long, userSession: Long)

class ApplicationConfig(val config: Config) {

  lazy val webServiceConfig: WebServiceConfig =
    WebServiceConfig(interface = config.getString("webservice.interface"), port = config.getInt("webservice.port"))

  lazy val authConfig: AuthConfig = {
    val result = AuthConfig(
      secretKey = config.getString("auth.secretKey"),
      hmacAlgorithm = {
        val hmacAlgorithm = config.getString("auth.hmacAlgorithm")
        JwtAlgorithm.fromString(hmacAlgorithm).asInstanceOf[JwtHmacAlgorithm]
      },
      expirationTimeInSeconds = ExpirationTimeInSeconds(
        accessToken = config.getLong("auth.expirationTimeInSeconds.accessToken"),
        refreshToken = config.getLong("auth.expirationTimeInSeconds.refreshToken"),
        userSession = config.getLong("auth.expirationTimeInSeconds.userSession")
      )
    )

    if (result.expirationTimeInSeconds.accessToken >= result.expirationTimeInSeconds.refreshToken) {
      throw new RuntimeException("Access token lifetime should be less than refresh token lifetime.")
    } else if (result.expirationTimeInSeconds.refreshToken >= result.expirationTimeInSeconds.userSession) {
      throw new RuntimeException("Refresh token lifetime should be less than user session lifetime.")
    }

    result
  }

}

object ApplicationConfig {
  def load(config: Config = ConfigFactory.load()): ApplicationConfig =
    new ApplicationConfig(config)
}
