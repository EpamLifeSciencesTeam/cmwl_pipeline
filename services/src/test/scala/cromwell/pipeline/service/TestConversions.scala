package cromwell.pipeline.service

import cromwell.pipeline.datastorage.dto._
import cromwell.pipeline.datastorage.dto.auth.AuthResponse

import scala.language.implicitConversions

trait TestConversions {

  implicit def fromException(exc: Throwable): DummyToReturn =
    WithException(exc)

  implicit def fromSuccessResponseMessages(messages: List[SuccessResponseMessage]): DummyToReturn =
    SuccessResponseMessagesToReturn(messages)

  implicit def fromProjectFiles(files: List[ProjectFile]): DummyToReturn =
    ProjectFilesToReturn(files)

  implicit def fromUpdateFiledResponse(response: UpdateFiledResponse): DummyToReturn =
    UpdateFiledResponseToReturn(response)

  implicit def fromProject(project: Project): DummyToReturn =
    ProjectToReturn(project)

  implicit def fromFileCommits(commits: Seq[FileCommit]): DummyToReturn =
    FileCommitsToReturn(commits)

  implicit def fromGitLabVersions(gitLabVersions: Seq[GitLabVersion]): DummyToReturn =
    GitLabVersionsToReturn(gitLabVersions)

  implicit def fromFileTrees(fileTrees: Seq[FileTree]): DummyToReturn =
    FileTreesToReturn(fileTrees)

  implicit def fromProjectFile(file: ProjectFile): DummyToReturn =
    ProjectFileToReturn(file)

  implicit def fromPipelineVersion(pipelineVersion: PipelineVersion): DummyToReturn =
    PipelineVersionToReturn(pipelineVersion)

  implicit def fromProjectConfiguration(projectConfiguration: ProjectConfiguration): DummyToReturn =
    ProjectConfigurationToReturn(projectConfiguration)

  implicit def fromAuthResponse(authResponse: AuthResponse): DummyToReturn =
    AuthResponseToReturn(authResponse)

  implicit def fromCromwellInput(cromwellInput: CromwellInput): DummyToReturn =
    CromwellInputToReturn(cromwellInput)

}

sealed trait DummyToReturn
sealed trait Failure extends DummyToReturn
sealed trait Success extends DummyToReturn

case class WithException(exc: Throwable) extends Failure

case class SuccessResponseMessagesToReturn(messages: List[SuccessResponseMessage]) extends Success
case class ProjectFilesToReturn(files: List[ProjectFile]) extends Success
case class UpdateFiledResponseToReturn(response: UpdateFiledResponse) extends Success
case class ProjectToReturn(project: Project) extends Success
case class FileCommitsToReturn(commits: Seq[FileCommit]) extends Success
case class GitLabVersionsToReturn(gitLabVersions: Seq[GitLabVersion]) extends Success
case class FileTreesToReturn(fileTrees: Seq[FileTree]) extends Success
case class ProjectFileToReturn(projectFile: ProjectFile) extends Success
case class PipelineVersionToReturn(pipelineVersion: PipelineVersion) extends Success
case class ProjectConfigurationToReturn(projectConfiguration: ProjectConfiguration) extends Success
case class AuthResponseToReturn(authResponse: AuthResponse) extends Success
case class CromwellInputToReturn(cromwellInput: CromwellInput) extends Success
case object NoneToReturn extends Success
