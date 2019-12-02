package cromwell.pipeline.utils.auth

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.typesafe.config.{ Config, ConfigFactory }

object TestContainersUtils {

  private val dbConfig = ConfigFactory.load().getConfig("database.postgres_dc.db.properties")
  private val databaseName = dbConfig.getString("databaseName")
  private val user = dbConfig.getString("user")
  private val password = dbConfig.getString("password")

  def getPostgreSQLContainer(postgresImageName: String = "postgres:12"): PostgreSQLContainer =
    PostgreSQLContainer(postgresImageName).configure { c =>
      c.withDatabaseName(s"$databaseName")
      c.withUsername(s"$user")
      c.withPassword(s"$password")
    }

  import scala.collection.JavaConverters._

  def getConfigForPgContainer(container: PostgreSQLContainer): Config = ConfigFactory
    .parseMap(
      Map(
        "database.postgres_dc.db.properties.portNumber" -> container.mappedPort(5432)
      ).asJava
    )
    .withFallback(ConfigFactory.load())
}
