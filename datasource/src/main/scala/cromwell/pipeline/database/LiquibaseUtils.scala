package cromwell.pipeline.database

import cromwell.pipeline.database.LiquibaseExceptions.LiquibaseUpdateSchemaException
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

  def updateSchema(
    changeLogResourcePath: String
  )(jdbcConnection: Connection): Try[Unit] = {
    val liquibaseConnection = new JdbcConnection(jdbcConnection)

    Try {
      val database = DatabaseFactory.getInstance.findCorrectDatabaseImplementation(liquibaseConnection)
      val liquibase = new Liquibase(changeLogResourcePath, new ClassLoaderResourceAccessor(), database)

      liquibase.update(DefaultContexts, DefaultLabelExpression)
    } match {
      case Failure(exception) =>
        liquibaseConnection.close()
        val liquibaseUpdateSchemaException = LiquibaseUpdateSchemaException(exception)
        log.error(liquibaseUpdateSchemaException.message, liquibaseUpdateSchemaException.cause)
        Failure(liquibaseUpdateSchemaException)
      case Success(_) =>
        liquibaseConnection.close()
        log.info("Successfully updated db schema via liquibase!")
        Success(())
    }
  }
}
