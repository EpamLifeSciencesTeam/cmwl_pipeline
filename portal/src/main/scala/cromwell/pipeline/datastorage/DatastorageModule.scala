package cromwell.pipeline.datastorage

import com.softwaremill.macwire._
import com.typesafe.config.Config
import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.{ ProjectEntry, UserEntry }
import cromwell.pipeline.datastorage.dao.repository.{ ProjectRepository, UserRepository }

@Module
class DatastorageModule(config: Config) {
  lazy val pipelineDatabaseEngine: PipelineDatabaseEngine = wireWith(PipelineDatabaseEngine.fromConfig _)
  lazy val userEntry: UserEntry = wire[UserEntry]
  lazy val userRepository: UserRepository = wire[UserRepository]
  lazy val projectEntry: ProjectEntry = wire[ProjectEntry]
  lazy val projectRepository: ProjectRepository = wire[ProjectRepository]
}
