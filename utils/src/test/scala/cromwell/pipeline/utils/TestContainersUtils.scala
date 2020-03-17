package cromwell.pipeline.utils

import com.dimafeng.testcontainers.PostgreSQLContainer
import com.typesafe.config.{ Config, ConfigFactory }

object TestContainersUtils {

  private val dbConfig = ConfigFactory.load().getConfig("database.postgres_dc.db.properties")
  private val user = dbConfig.getString("user")
  private val databaseName = dbConfig.getString("databaseName")
  private val password = dbConfig.getString("password")
  private val port = dbConfig.getInt("portNumber")

  def getPostgreSQLContainer(postgresImageName: String = "postgres:12"): PostgreSQLContainer =
    PostgreSQLContainer(
      dockerImageNameOverride = postgresImageName,
      databaseName = databaseName,
      username = user,
      password = password
    )

  import scala.jdk.CollectionConverters._

  def getConfigForPgContainer(container: PostgreSQLContainer): Config = ConfigFactory
    .parseMap(
      Map(
        "database.postgres_dc.db.properties.portNumber" -> container.mappedPort(port)
      ).asJava
    )
    .withFallback(ConfigFactory.load())
}
