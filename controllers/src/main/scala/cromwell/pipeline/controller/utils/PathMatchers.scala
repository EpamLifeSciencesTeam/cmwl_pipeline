package cromwell.pipeline.controller.utils

import akka.http.scaladsl.server.PathMatcher1
import akka.http.scaladsl.server.PathMatchers.Segment
import cromwell.pipeline.datastorage.dto
import cromwell.pipeline.model.wrapper

import java.nio.file.{ Path, Paths }

object PathMatchers {
  val ProjectId: PathMatcher1[dto.ProjectId] = Segment.map(dto.ProjectId(_))
  val Path: PathMatcher1[Path] = Segment.map(Paths.get(_))
  val RunId: PathMatcher1[wrapper.RunId] = Segment.flatMap(wrapper.RunId.from(_).toOption)
  val ProjectSearchFilterId: PathMatcher1[wrapper.ProjectSearchFilterId] =
    Segment.flatMap(wrapper.ProjectSearchFilterId.from(_).toOption)
}
