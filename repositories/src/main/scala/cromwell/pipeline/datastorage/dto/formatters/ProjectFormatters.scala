package cromwell.pipeline.datastorage.dto.formatters

import java.nio.file.Path

import cromwell.pipeline.datastorage.dto.UserId
import slick.lifted.MappedTo

object ProjectFormatters {

  final case class ProjectId(value: String) extends MappedTo[String]

  final case class ProjectAdditionRequest(ownerId: UserId, name: String, repository: String)

  final case class Version(value: String) extends AnyVal

  final case class ProjectFile(path: Path, content: String)

}
