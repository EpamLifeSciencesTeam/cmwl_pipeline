package cromwell.pipeline.utils

import com.dimafeng.testcontainers.{ MongoDBContainer, PostgreSQLContainer }
import com.typesafe.config.{ Config, ConfigFactory }
import org.testcontainers.utility.DockerImageName

object TestContainersUtils {

  private lazy val postgreConfig = ConfigFactory.load().getConfig("database.postgres_dc.db.properties")

  def getPostgreSQLContainer(postgresImageName: String = "postgres:12"): PostgreSQLContainer =
    PostgreSQLContainer(
      dockerImageNameOverride = DockerImageName.parse(postgresImageName),
      databaseName = postgreConfig.getString("databaseName"),
      username = postgreConfig.getString("user"),
      password = postgreConfig.getString("password")
    )

  def getConfigForPgContainer(container: PostgreSQLContainer): Config =
    config(
      "database.postgres_dc.db.properties.portNumber" ->
        container.mappedPort(postgreConfig.getInt("portNumber"))
    )

  private lazy val mongoConfig = ConfigFactory.load().getConfig("database.mongo")
  private def mongoPort: Int = mongoConfig.getInt("port")

  def getMongoContainer(mongoImageName: String = "mongo"): MongoDBContainer =
    MongoDBContainer(DockerImageName.parse(mongoImageName))

  def getConfigForMongoContainer(container: MongoDBContainer): MongoConfig =
    MongoConfig(
      user = mongoConfig.getString("user"),
      password = mongoConfig.getString("password").toCharArray,
      host = mongoConfig.getString("host"),
      port = container.mappedPort(mongoPort),
      authenticationDatabase = mongoConfig.getString("authenticationDatabase"),
      database = mongoConfig.getString("database"),
      collection = mongoConfig.getString("collection")
    )

  import scala.collection.JavaConverters._

  private def config(pairs: (String, Any)*): Config =
    ConfigFactory.parseMap(pairs.toMap.asJava).withFallback(ConfigFactory.load())
}
