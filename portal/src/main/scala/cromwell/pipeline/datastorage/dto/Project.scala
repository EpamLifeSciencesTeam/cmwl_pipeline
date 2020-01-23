package cromwell.pipeline.datastorage.dto

import slick.lifted.MappedTo

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  description: String,
  repository: String,
  active: Boolean
)

final case class ProjectId(value: String) extends MappedTo[String]

final case class ProjectAdditionRequest(ownerId: UserId, name: String, description: String, repository: String)

final case class Version(value: String) extends AnyVal
