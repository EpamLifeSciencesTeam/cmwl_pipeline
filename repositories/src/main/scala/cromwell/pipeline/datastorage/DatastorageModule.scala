package cromwell.pipeline.datastorage

import com.typesafe.config.Config
import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.ProjectEntry
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dao.repository.{ ProjectRepository, UserRepository }
import cromwell.pipeline.datastorage.utils.auth.{ AuthUtils, SecurityDirective }
import cromwell.pipeline.utils.ApplicationConfig
import slick.jdbc.JdbcProfile

class DatastorageModule(applicationConfig: ApplicationConfig) {

  lazy val authUtils: AuthUtils = new AuthUtils(applicationConfig.authConfig)
  lazy val securityDirective: SecurityDirective = new SecurityDirective(applicationConfig.authConfig)
  lazy val pipelineDatabaseEngine: PipelineDatabaseEngine = new PipelineDatabaseEngine(applicationConfig.config)
  lazy val profile: JdbcProfile = pipelineDatabaseEngine.profile
  lazy val databaseLayer: DatabaseLayer = new DatabaseLayer(profile)

  lazy val userRepository: UserRepository =
    new UserRepository(pipelineDatabaseEngine, databaseLayer)
  lazy val projectRepository: ProjectRepository =
    new ProjectRepository(pipelineDatabaseEngine, databaseLayer)
}

trait Profile {
  val profile: JdbcProfile
}

class DatabaseLayer(val profile: JdbcProfile) extends Profile with UserEntry with ProjectEntry
