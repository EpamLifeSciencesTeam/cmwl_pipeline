package cromwell.pipeline.service

import akka.actor.ActorSystem
import cromwell.pipeline.utils.FiltersCleanupConfig

import java.time.Instant
import scala.concurrent.ExecutionContext

trait ProjectSearchFilterCleaner {

  def scheduleFiltersCleanup(): Unit

}

object ProjectSearchFilterCleaner {

  def apply(
    projectSearchFilterService: ProjectSearchFilterService,
    filtersCleanupConfig: FiltersCleanupConfig,
    system: ActorSystem
  )(
    implicit executionContext: ExecutionContext
  ): ProjectSearchFilterCleaner =
    new ProjectSearchFilterCleaner {

      import filtersCleanupConfig._

      override def scheduleFiltersCleanup(): Unit =
        system.scheduler.scheduleAtFixedRate(interval, interval, getFiltersCleanRun, executionContext)

      private def getFiltersCleanRun: Runnable = () => {
        val usedLaterThan = Instant.now.minus(timeToLive)
        projectSearchFilterService.deleteOldFilters(usedLaterThan)
      }
    }
}
