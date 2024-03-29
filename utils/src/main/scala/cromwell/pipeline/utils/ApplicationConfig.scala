package cromwell.pipeline.utils

import com.typesafe.config.{ Config, ConfigFactory }
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm

import java.time.Duration

sealed trait ConfigComponent

final case class WebServiceConfig(
  interface: String,
  port: Int,
  cors: CorsConfig
) extends ConfigComponent

final case class GitLabConfig(
  url: String,
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

final case class PostgreConfig(
  serverName: String,
  portNumber: Int,
  databaseName: String,
  user: String,
  password: Array[Char]
) extends ConfigComponent

final case class ServiceConfig(filtersCleanup: FiltersCleanupConfig) extends ConfigComponent

final case class CorsConfig(allowedOrigins: Seq[String]) extends ConfigComponent

final case class ExpirationTimeInSeconds(accessToken: Long, refreshToken: Long, userSession: Long)

final case class FiltersCleanupConfig(timeToLive: Duration, interval: Duration) extends ConfigComponent

class ApplicationConfig(val config: Config) {

  import scala.collection.JavaConverters._

  lazy val webServiceConfig: WebServiceConfig = {
    val _config = config.getConfig("webservice")
    WebServiceConfig(
      interface = _config.getString("interface"),
      port = _config.getInt("port"),
      cors = CorsConfig(_config.getStringList("cors.allowedOrigins").asScala)
    )
  }

  lazy val authConfig: AuthConfig = {
    val _config = config.getConfig("auth")
    val expirationTime = _config.getConfig("expirationTimeInSeconds")

    val result = AuthConfig(
      secretKey = _config.getString("secretKey"),
      hmacAlgorithm = {
        val hmacAlgorithm = _config.getString("hmacAlgorithm")
        JwtAlgorithm.fromString(hmacAlgorithm).asInstanceOf[JwtHmacAlgorithm]
      },
      expirationTimeInSeconds = ExpirationTimeInSeconds(
        accessToken = expirationTime.getLong("accessToken"),
        refreshToken = expirationTime.getLong("refreshToken"),
        userSession = expirationTime.getLong("userSession")
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
    val _config = config.getConfig("database.gitlab")
    GitLabConfig(
      url = _config.getString("url"),
      token = Map("PRIVATE-TOKEN" -> _config.getString("token")),
      defaultFileVersion = _config.getString("defaultFileVersion"),
      defaultBranch = _config.getString("defaultBranch")
    )
  }

  lazy val mongoConfig: MongoConfig = {
    val _config = config.getConfig("database.mongo")
    MongoConfig(
      user = _config.getString("user"),
      password = _config.getString("password").toCharArray,
      host = _config.getString("host"),
      port = _config.getInt("port"),
      authenticationDatabase = _config.getString("authenticationDatabase"),
      database = _config.getString("database"),
      collection = _config.getString("collection")
    )
  }

  lazy val postgreConfig: PostgreConfig = {
    val _config = config.getConfig("database.postgres_dc.db.properties")
    PostgreConfig(
      serverName = _config.getString("serverName"),
      portNumber = _config.getInt("portNumber"),
      databaseName = _config.getString("databaseName"),
      user = _config.getString("user"),
      password = _config.getString("password").toCharArray
    )
  }

  lazy val serviceConfig: ServiceConfig = {
    val _config = config.getConfig("service")
    val filtersCleanup = _config.getConfig("filtersCleanup")
    ServiceConfig(
      filtersCleanup = FiltersCleanupConfig(
        timeToLive = filtersCleanup.getDuration("timeToLive"),
        interval = filtersCleanup.getDuration("interval")
      )
    )
  }

}

object ApplicationConfig {
  def load(config: Config = ConfigFactory.load()): ApplicationConfig =
    new ApplicationConfig(config)

  def unapply(
    config: ApplicationConfig
  ): Option[(WebServiceConfig, AuthConfig, GitLabConfig, MongoConfig, PostgreConfig, ServiceConfig)] = Option(
    (
      config.webServiceConfig,
      config.authConfig,
      config.gitLabConfig,
      config.mongoConfig,
      config.postgreConfig,
      config.serviceConfig
    )
  )

}
