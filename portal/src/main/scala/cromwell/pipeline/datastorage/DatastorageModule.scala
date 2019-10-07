package cromwell.pipeline.datastorage

import com.softwaremill.macwire.{wire, wireWith}
import com.typesafe.config.{Config, ConfigFactory}
import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.UserEntry
import cromwell.pipeline.datastorage.dao.repository.UserRepository

trait DatastorageModule {
  lazy val config: Config = ConfigFactory.load()
  lazy val pipelineDatabaseEngine: PipelineDatabaseEngine = wireWith(PipelineDatabaseEngine.fromConfig _)
  lazy val userEntry: UserEntry = wire[UserEntry]
  lazy val userRepository: UserRepository = wire[UserRepository]
}
