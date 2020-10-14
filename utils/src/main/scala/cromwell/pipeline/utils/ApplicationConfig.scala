package cromwell.pipeline.utils

import com.typesafe.config.{ Config, ConfigFactory }
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm

sealed trait ConfigComponent

final case class WebServiceConfig(interface: String, port: Int) extends ConfigComponent

final case class GitLabConfig(
  url: String,
  idPath: String,
  token: Map[String, String],
  defaultFileVersion: String,
  defaultBranch: String
) extends ConfigComponent

final case class AuthConfig(
  secretKey: String,
  hmacAlgorithm: JwtHmacAlgorithm,
  expirationTimeInSeconds: ExpirationTimeInSeconds
) extends ConfigComponent

final case class MongoConfig(
  user: String,
  password: Array[Char],
  host: String,
  port: Int,
  authenticationDatabase: String,
  database: String,
  collection: String
) extends ConfigComponent

final case class CromwellConfig(
  host: String,
  version: String,
  enginePath: String
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

  lazy val gitLabConfig: GitLabConfig = {
    GitLabConfig(
      url = config.getString("database.gitlab.url"),
      idPath = config.getString("database.gitlab.path") + "%2F",
      token = Map("PRIVATE-TOKEN" -> config.getString("database.gitlab.token")),
      defaultFileVersion = config.getString("database.gitlab.defaultFileVersion"),
      defaultBranch = config.getString("database.gitlab.defaultBranch")
    )
  }

  lazy val mongoConfig: MongoConfig = {
    MongoConfig(
      user = config.getString("database.mongo.user"),
      password = config.getString("database.mongo.password").toCharArray,
      host = config.getString("database.mongo.host"),
      port = config.getInt("database.mongo.port"),
      authenticationDatabase = config.getString("database.mongo.authenticationDatabase"),
      database = config.getString("database.mongo.database"),
      collection = config.getString("database.mongo.collection")
    )
  }

  lazy val cromwellConfig: CromwellConfig =
    CromwellConfig(
      host = config.getString("cromwell-backend.host"),
      version = config.getString("cromwell-backend.version"),
      enginePath = config.getString("cromwell-backend.enginePath")
    )
}

object ApplicationConfig {
  def load(config: Config = ConfigFactory.load()): ApplicationConfig =
    new ApplicationConfig(config)
}
