package cromwell.pipeline.database

import java.sql.Connection

import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.{ Contexts, LabelExpression, Liquibase }

import scala.util.Try

object LiquibaseUtils {

  private val DefaultContexts = new Contexts()
  private val DefaultLabelExpression = new LabelExpression()

  def updateSchema(changeLogResourcePath: String)(jdbcConnection: Connection): Unit = {
    val liquibaseConnection = new JdbcConnection(jdbcConnection)
    Try {
      val database = DatabaseFactory.getInstance.findCorrectDatabaseImplementation(liquibaseConnection)
      val liquibase = new Liquibase(changeLogResourcePath, new ClassLoaderResourceAccessor(), database)

      liquibase.update(DefaultContexts, DefaultLabelExpression)
    }.foreach { _ =>
      liquibaseConnection.close()
    }
  }
}
