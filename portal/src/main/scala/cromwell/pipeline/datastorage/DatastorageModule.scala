package cromwell.pipeline.datastorage

import com.softwaremill.macwire._
import com.typesafe.config.Config
import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dao.repository.UserRepository

@Module
class DatastorageModule(config: Config) {
  lazy val pipelineDatabaseEngine: PipelineDatabaseEngine = wireWith(PipelineDatabaseEngine.fromConfig _)
  lazy val userEntry: UserEntry = wire[UserEntry]
  lazy val userRepository: UserRepository = wire[UserRepository]
}
