package cromwell.pipeline.datastorage.dto

import slick.lifted.MappedTo

final case class Project(
  projectId: ProjectId,
  ownerId: UUID,
  name: String,
  repository: String,
  active: Boolean
)

final case class ProjectId(value: String) extends MappedTo[String]

final case class ProjectAdditionRequest(ownerId: UUID, name: String, repository: String)
