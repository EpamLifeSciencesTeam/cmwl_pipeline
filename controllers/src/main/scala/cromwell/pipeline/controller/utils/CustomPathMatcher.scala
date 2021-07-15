package cromwell.pipeline.controller.utils

import akka.http.scaladsl.server
import akka.http.scaladsl.server.PathMatcher
import akka.http.scaladsl.server.PathMatchers.Segment
import cromwell.pipeline.datastorage.dto

import java.nio.file.{ Path, Paths }

object CustomPathMatcher {
  val ProjectId: server.PathMatcher1[dto.ProjectId] = PathMatcher(Segment).map(s => dto.ProjectId(s))
  val ProjectFIlePath: server.PathMatcher1[Path] = PathMatcher(Segment).map(s => Paths.get(s))
}
