package cromwell.pipeline.datastorage.dto

import java.nio.file.Path

import cromwell.pipeline.model.wrapper.UserId
import slick.lifted.MappedTo

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  active: Boolean,
  repository: Option[Repository] = None,
  visibility: Visibility = Private
) {
  def withRepository(repositoryPath: Option[String]): Project =
    this.copy(repository = repositoryPath.map(Repository))
}

final case class ProjectId(value: String) extends MappedTo[String]

final case class Repository(value: String) extends MappedTo[String]

final case class ProjectAdditionRequest(name: String)

final case class ProjectDeleteRequest(projectId: ProjectId)

final case class ProjectUpdateRequest(projectId: ProjectId, name: String, repository: Option[Repository])

final case class ProjectResponse(projectId: ProjectId, name: String, active: Boolean)

final case class Version(name: String, message: String, target: String, commit: Commit)

final case class Commit(id: String)

final case class ProjectFile(path: Path, content: String)

sealed trait Visibility
case object Private extends Visibility
case object Internal extends Visibility
case object Public extends Visibility

object Visibility {
  def fromString(s: String): Visibility = s match {
    case "private"  => Private
    case "internal" => Internal
    case "public"   => Public
  }

  def toString(visibility: Visibility): String = visibility match {
    case Private  => "private"
    case Internal => "internal"
    case Public   => "public"
  }

  def values = Seq(Private, Internal, Public)
}

final case class FileContent(content: String)

final case class ProjectUpdateFileRequest(project: Project, projectFile: ProjectFile, version: Option[Version])

final case class UpdateFileRequest(branch: String, content: String, commitMessage: String)
