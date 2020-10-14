package cromwell.pipeline.utils

import com.typesafe.config.{ Config, ConfigFactory }
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm
import play.api.libs.json.{ JsObject, Json, Writes }

sealed trait ConfigComponent
object ConfigComponent {
  implicit val webserviceWrites = new Writes[WebServiceConfig] {
    def writes(config: WebServiceConfig) = Json.obj(
      "interface" -> config.interface,
      "port" -> config.port
    )
  }

  implicit val gitlabWrites = new Writes[GitLabConfig] {
    def writes(config: GitLabConfig) = Json.obj(
      "url" -> config.url,
      "idPath" -> config.idPath,
      "token" -> config.token.map { case (k, v) => k -> v.map(_ => '*') },
      "defaultFileVersion" -> config.defaultFileVersion,
      "defaultBranch" -> config.defaultBranch
    )
  }

  implicit val expTimeWrites = new Writes[ExpirationTimeInSeconds] {
    def writes(token: ExpirationTimeInSeconds) = Json.obj(
      "accessToken" -> token.accessToken,
      "refreshToken" -> token.refreshToken,
      "userSession" -> token.userSession
    )
  }

  implicit val authWrites = new Writes[AuthConfig] {
    def writes(config: AuthConfig) = Json.obj(
      "secretKey" -> config.secretKey.map(_ => '*'),
      "hmacAlgorithm" -> config.hmacAlgorithm.toString,
      "expirationTimeInSeconds" -> Json.toJson(config.expirationTimeInSeconds)
    )
  }

  implicit val mongoWrites = new Writes[MongoConfig] {
    def writes(config: MongoConfig) = Json.obj(
      "user" -> config.user,
      "password" -> config.password.map(_ => '*'),
      "host" -> config.host,
      "port" -> config.port,
      "authenticationDatabase" -> config.authenticationDatabase,
      "database" -> config.database,
      "collection" -> config.collection
    )
  }
  implicit val postgresWrites = new Writes[PostgresConfig] {
    def writes(config: PostgresConfig) = Json.obj(
      "user" -> config.user,
      "password" -> config.password.map(_ => '*'),
      "host" -> config.host,
      "port" -> config.port,
      "database" -> config.database
    )
  }
}

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

final case class PostgresConfig(
  user: String,
  password: String,
  host: String,
  port: Int,
  database: String
) extends ConfigComponent

final case class ExpirationTimeInSeconds(accessToken: Long, refreshToken: Long, userSession: Long)

class ApplicationConfig(val config: Config) {

  lazy val webServiceConfig: WebServiceConfig = {
    val _config = config.getConfig("webservice")
    WebServiceConfig(interface = _config.getString("interface"), port = _config.getInt("port"))
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
      idPath = _config.getString("path") + "%2F",
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

  lazy val postgresConfig: PostgresConfig = {
    PostgresConfig(
      user = config.getString("database.postgres_dc.db.properties.user"),
      password = config.getString("database.postgres_dc.db.properties.password"),
      host = config.getString("database.postgres_dc.db.properties.serverName"),
      port = config.getInt("database.postgres_dc.db.properties.portNumber"),
      database = config.getString("database.postgres_dc.db.properties.databaseName")
    )
  }
}

object ApplicationConfig {
  def load(config: Config = ConfigFactory.load()): ApplicationConfig =
    new ApplicationConfig(config)
}
