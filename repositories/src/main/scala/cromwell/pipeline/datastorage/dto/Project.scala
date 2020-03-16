package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import slick.lifted.MappedTo

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  repository: String,
  active: Boolean
)

final case class ProjectId(value: String) extends MappedTo[String]

final case class ProjectAdditionRequest(ownerId: UserId, name: String, repository: String)

final case class Version(value: String) extends AnyVal

final case class ProjectFile(path: Path, content: String)
