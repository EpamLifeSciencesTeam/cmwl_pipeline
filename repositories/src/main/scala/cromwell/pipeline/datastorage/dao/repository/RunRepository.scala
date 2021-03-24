package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.RunEntry
import cromwell.pipeline.datastorage.dto.Run
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

import scala.concurrent.Future

class RunRepository(pipelineDatabaseEngine: PipelineDatabaseEngine, runEntry: RunEntry) {

  import pipelineDatabaseEngine._
  import pipelineDatabaseEngine.profile.api._

  def getRunByIdAndUser(runId: RunId, userId: UserId): Future[Option[Run]] =
    database.run(runEntry.getRunByIdAndUser(runId, userId).result.headOption)

  def deleteRunById(runId: RunId): Future[Int] = database.run(runEntry.deleteRunById(runId))

  def addRun(run: Run): Future[RunId] = database.run(runEntry.addRun(run))

  def updateRun(updatedRun: Run): Future[Int] = database.run(runEntry.updateRun(updatedRun))
}
