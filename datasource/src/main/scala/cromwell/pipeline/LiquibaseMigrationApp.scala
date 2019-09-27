package cromwell.pipeline

import cromwell.pipeline.database.PipelineDatabaseEngine

object LiquibaseMigrationApp extends App {
  val pipelineDatabaseEngine = PipelineDatabaseEngine.fromConfig()
  pipelineDatabaseEngine.updateSchema()
}
