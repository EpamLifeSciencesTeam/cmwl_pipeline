package cromwell.pipeline.database

import java.sql.Connection

import com.typesafe.config.{ Config, ConfigFactory }
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PipelineDatabaseEngine(config: Config = ConfigFactory.load()) extends AutoCloseable {

  private val slickConfig = DatabaseConfig.forConfig[JdbcProfile](path = "database.postgres_dc", config)
  private val changeLogResourcePath = config.getString("database.liquibase.changeLogResourcePath")

  val profile = slickConfig.profile
  val database = slickConfig.db

  import profile.api._

  def updateSchema(): Unit =
    withConnection(LiquibaseUtils.updateSchema(changeLogResourcePath))

  private def withConnection[A](actionFunc: Connection => A): A = {
    val action = SimpleDBIO(context => actionFunc(context.connection))
    Await.result(database.run(action), Duration.Inf)
  }

  override def close(): Unit =
    database.close()
}

object PipelineDatabaseEngine {
  def fromConfig(config: Config = ConfigFactory.load()): PipelineDatabaseEngine =
    new PipelineDatabaseEngine(config)
}
