package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.RunEntry
import cromwell.pipeline.datastorage.dto.Run
import cromwell.pipeline.model.wrapper.{RunId}

import scala.concurrent.Future

class RunRepository(pipelineDatabaseEngine: PipelineDatabaseEngine, runEntry: RunEntry) {

  import pipelineDatabaseEngine._
  import pipelineDatabaseEngine.profile.api._

  def getRunById(runId: RunId): Future[Option[Run]] =
    database.run(runEntry.getRunById(runId).result.headOption)

  def deleteRunById(runId: RunId): Future[Int] = database.run(runEntry.deleteRunById(runId))

  def addRun(run: Run): Future[RunId] = database.run(runEntry.addRun(run))

  def updateRun(updatedRun: Run): Future[Int] = database.run(runEntry.updateRun(updatedRun))
}
