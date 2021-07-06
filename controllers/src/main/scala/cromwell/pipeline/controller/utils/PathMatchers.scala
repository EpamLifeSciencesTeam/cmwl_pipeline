package cromwell.pipeline.controller.utils

import akka.http.scaladsl.server.PathMatcher1
import akka.http.scaladsl.server.PathMatchers.Segment
import cromwell.pipeline.datastorage.dto

import java.nio.file.{ Path, Paths }

object PathMatchers {
  val ProjectId: PathMatcher1[dto.ProjectId] = Segment.map(dto.ProjectId(_))
  val Path: PathMatcher1[Path] = Segment.map(Paths.get(_))
}
