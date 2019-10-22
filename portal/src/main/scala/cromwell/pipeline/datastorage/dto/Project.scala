package cromwell.pipeline.datastorage.dto

import slick.lifted.MappedTo

final case class Project(projectId: ProjectId, name: String, description: String, repository: String)

final case class ProjectId(value: String) extends MappedTo[String]
