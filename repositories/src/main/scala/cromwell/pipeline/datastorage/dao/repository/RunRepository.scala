package cromwell.pipeline.datastorage.dao.repository

import cromwell.pipeline.database.PipelineDatabaseEngine
import cromwell.pipeline.datastorage.dao.entry.RunEntry
import cromwell.pipeline.datastorage.dto.{ ProjectId, Run }
import cromwell.pipeline.model.wrapper.{ RunId, UserId }

import scala.concurrent.Future

trait RunRepository {

  def getRunByIdAndUser(runId: RunId, userId: UserId): Future[Option[Run]]

  def getRunsByProject(projectId: ProjectId): Future[Seq[Run]]

  def deleteRunById(runId: RunId): Future[Int]

  def addRun(run: Run): Future[RunId]

  def updateRun(updatedRun: Run): Future[Int]

}

object RunRepository {

  def apply(pipelineDatabaseEngine: PipelineDatabaseEngine, runEntry: RunEntry): RunRepository =
    new RunRepository {

      import pipelineDatabaseEngine._

      import pipelineDatabaseEngine.profile.api._

      def getRunByIdAndUser(runId: RunId, userId: UserId): Future[Option[Run]] =
        database.run(runEntry.getRunByIdAndUser(runId, userId).result.headOption)

      def getRunsByProject(projectId: ProjectId): Future[Seq[Run]] =
        database.run(runEntry.getRunsByProject(projectId).result)

      def deleteRunById(runId: RunId): Future[Int] = database.run(runEntry.deleteRunById(runId))

      def addRun(run: Run): Future[RunId] = database.run(runEntry.addRun(run))

      def updateRun(updatedRun: Run): Future[Int] = database.run(runEntry.updateRun(updatedRun))

    }

}
