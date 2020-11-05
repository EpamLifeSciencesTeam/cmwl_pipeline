package cromwell.pipeline.utils

import com.dimafeng.testcontainers.{ MongoDBContainer, PostgreSQLContainer }
import com.typesafe.config.{ Config, ConfigFactory }

object TestContainersUtils {

  private val pgConfig = ConfigFactory.load().getConfig("database.postgres_dc.db.properties")
  private val pgUser = pgConfig.getString("user")
  private val pgDatabaseName = pgConfig.getString("databaseName")
  private val pgPassword = pgConfig.getString("password")
  private val pgPort = pgConfig.getInt("portNumber")

  private val mongoConfig = ConfigFactory.load().getConfig("database.mongo")
  private val mongoPort = mongoConfig.getInt("port")

  def getPostgreSQLContainer(postgresImageName: String = "postgres:12"): PostgreSQLContainer =
    PostgreSQLContainer(
      dockerImageNameOverride = postgresImageName,
      databaseName = pgDatabaseName,
      username = pgUser,
      password = pgPassword
    )

  def getMongoDBContainer(mongoImageName: String = "mongo"): MongoDBContainer =
    MongoDBContainer(mongoImageName)

  import scala.collection.JavaConverters._

  def getConfigForPgContainer(container: PostgreSQLContainer): Config = ConfigFactory
    .parseMap(
      Map(
        "database.postgres_dc.db.properties.portNumber" -> container.mappedPort(pgPort)
      ).asJava
    )
    .withFallback(ConfigFactory.load())

  def getConfigForMongoDBContainer(container: MongoDBContainer): Config = ConfigFactory
    .parseMap(
      Map(
        "database.mongo" -> container.mappedPort(mongoPort)
      ).asJava
    )
    .withFallback(ConfigFactory.load())
}
