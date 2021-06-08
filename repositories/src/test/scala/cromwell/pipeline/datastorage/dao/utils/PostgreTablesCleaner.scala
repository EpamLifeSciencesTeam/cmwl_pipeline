package cromwell.pipeline.datastorage.dao.utils

import com.typesafe.config.Config
import cromwell.pipeline.database.LiquibaseUtils
import cromwell.pipeline.datastorage.DatastorageModule
import cromwell.pipeline.utils.TestTimeout
import org.scalatest.{ BeforeAndAfterEach, Suite }
import slick.jdbc.JdbcProfile

import scala.concurrent.Await

trait PostgreTablesCleaner extends BeforeAndAfterEach with TestTimeout {

  this: Suite =>

  protected def config: Config
  protected def datastorageModule: DatastorageModule
  protected def changeLogCleanTablesResourcePath: String =
    config.getString("database.liquibase.changeLogCleanTablesResourcePath")

  private lazy val profile = datastorageModule.pipelineDatabaseEngine.profile
  private lazy val database = datastorageModule.pipelineDatabaseEngine.database

  def cleanTables(profile: JdbcProfile, database: JdbcProfile#Backend#Database): Unit = {
    val action = profile.api.SimpleDBIO(
      context => LiquibaseUtils.updateSchema(changeLogCleanTablesResourcePath)(context.connection)
    )
    Await.result(database.run(action), timeoutAsDuration)
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    cleanTables(profile, database)
  }

}
