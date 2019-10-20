package cromwell.pipeline

import com.dimafeng.testcontainers.{Container, PostgreSQLContainer}
import com.typesafe.config.ConfigFactory

import scala.collection.convert.ImplicitConversions._

object BaseItTest {

  private val dbConfig = ConfigFactory.load().getConfig("database.postgres_dc.db.properties")
  private val databaseName = dbConfig.getString("databaseName")
  private val user = dbConfig.getString("user")
  private val password = dbConfig.getString("password")
  private val portNumber = dbConfig.getString("portNumber")

  def getPostgreSQLContainer(postgresImageName: String = "postgres:12"): Container = {
    PostgreSQLContainer(postgresImageName).configure { c =>
      c.withDatabaseName(s"$databaseName")
      c.withUsername(s"$user")
      c.withPassword(s"$password")
      c.setPortBindings(Seq(s"$portNumber:5432"))
    }
  }
}
