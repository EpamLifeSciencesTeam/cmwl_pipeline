package cromwell.pipeline.datastorage

import com.softwaremill.macwire._
import com.typesafe.config.Config
import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.{ ProjectEntry, UserEntry }
import cromwell.pipeline.datastorage.dao.repository.{ ProjectRepository, UserRepository }
import slick.jdbc.JdbcProfile

@Module
class DatastorageModule(config: Config) {
  lazy val pipelineDatabaseEngine: PipelineDatabaseEngine = wireWith(PipelineDatabaseEngine.fromConfig _)
  lazy val profile: JdbcProfile = pipelineDatabaseEngine.profile
  lazy val databaseLayer: DatabaseLayer = wire[DatabaseLayer]

  lazy val userRepository: UserRepository = wire[UserRepository] //wires databaseLayer
  lazy val projectRepository: ProjectRepository = wire[ProjectRepository] //wires databaseLayer
}

trait Profile {
  val profile: JdbcProfile
}

// https://books.underscore.io/essential-slick/essential-slick-3.html#scaling-to-larger-codebases
class DatabaseLayer(val profile: JdbcProfile) extends Profile with UserEntry with ProjectEntry
