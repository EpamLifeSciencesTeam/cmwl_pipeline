package cromwell.pipeline.database

import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.{ Contexts, LabelExpression, Liquibase }
import org.slf4j.{ Logger, LoggerFactory }

import java.sql.Connection
import scala.util.{ Failure, Success, Try }

object LiquibaseUtils {

  val log: Logger = LoggerFactory.getLogger(LiquibaseUtils.getClass)
  private val DefaultContexts = new Contexts()
  private val DefaultLabelExpression = new LabelExpression()

  def updateSchema(changeLogResourcePath: String)(jdbcConnection: Connection): Unit = {
    val liquibaseConnection = new JdbcConnection(jdbcConnection)
    Try {
      val database = DatabaseFactory.getInstance.findCorrectDatabaseImplementation(liquibaseConnection)
      val liquibase = new Liquibase(changeLogResourcePath, new ClassLoaderResourceAccessor(), database)

      liquibase.update(DefaultContexts, DefaultLabelExpression)
    } match {
      case Failure(exception) => log.error("An error during the schema update via liquibase.", exception)
      case Success(_)         => log.error("Successfully updated db schema via liquibase!")
    }
    liquibaseConnection.close()
  }
}
