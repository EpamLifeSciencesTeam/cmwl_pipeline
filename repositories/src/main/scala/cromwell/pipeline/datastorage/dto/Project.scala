package cromwell.pipeline.datastorage.dto

import java.nio.file.{ Path, Paths }

import cats.data.Validated
import cats.implicits._
import cromwell.pipeline.model.wrapper.{ UserId, VersionValue }
import play.api.libs.functional.syntax._
import play.api.libs.json._
import slick.lifted.MappedTo
import cromwell.pipeline.datastorage.formatters.ProjectFormatters._

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

final case class PostProject(name: String)

object PostProject {
  implicit lazy val postProject: OFormat[PostProject] = Json.format[PostProject]
}

final case class ProjectId(value: String) extends MappedTo[String]

final case class Repository(value: String) extends MappedTo[String]

final case class ProjectAdditionRequest(name: String)

final case class ProjectDeleteRequest(projectId: ProjectId)

final case class ProjectUpdateRequest(projectId: ProjectId, name: String, repository: Option[Repository])

final case class ProjectResponse(projectId: ProjectId, name: String, active: Boolean)

final case class RepositoryId(id: String)

object RepositoryId {
  implicit val gitlabProjectFormat: OFormat[RepositoryId] = Json.format[RepositoryId]
}

final case class GitLabVersion(name: PipelineVersion, message: String, target: String, commit: Commit)

object GitLabVersion {
  implicit val gitlabVersionFormat: OFormat[GitLabVersion] = Json.format[GitLabVersion]
}

final case class PipelineVersion(major: VersionValue, minor: VersionValue, revision: VersionValue)
    extends Ordered[PipelineVersion] {
  import VersionValue._
  def name: String = s"v$major.$minor.$revision"

  private val ordering: Ordering[PipelineVersion] = Ordering.by(v => (v.major, v.minor, v.revision))

  override def compare(that: PipelineVersion): Int =
    ordering.compare(this, that)

  def increaseMajor: PipelineVersion =
    this.copy(major = increment(this.major))

  def increaseMinor: PipelineVersion =
    this.copy(minor = increment(this.minor))

  def increaseRevision: PipelineVersion =
    this.copy(revision = increment(this.revision))

  override def toString: String = this.name
}

object PipelineVersion {
  import VersionValue._

  private val pattern = "^v(.+)\\.(.+)\\.(.+)$".r
  def apply(versionLine: String): PipelineVersion =
    versionLine match {
      case pattern(v1, v2, v3) =>
        val validationResult = (fromString(v1), fromString(v2), fromString(v3)).mapN(PipelineVersion.apply)
        validationResult match {
          case Validated.Valid(content)  => content
          case Validated.Invalid(errors) => throw PipelineVersionException(errors.toString)
        }
      case _ => throw PipelineVersionException(s"Format of version name: 'v(int).(int).(int)', but got: $versionLine")
    }

  final case class PipelineVersionException(message: String) extends Exception

  implicit val pipelineVersionFormat: Format[PipelineVersion] =
    implicitly[Format[String]].inmap(PipelineVersion.apply, _.name)
}

final case class Commit(id: String)

final case class FileCommit(commitId: String)
object FileCommit {
  implicit val fileCommitFormat: OFormat[FileCommit] = Json.format[FileCommit]
}

final case class ProjectFile(path: Path, content: ProjectFileContent)
final case class FileTree(id: String, name: String, path: String, mode: String)
object FileTree {
  implicit val fileTreeFormat: OFormat[FileTree] = Json.format[FileTree]
}

object ProjectFile {

  implicit object pathFormat extends Format[Path] {
    override def writes(o: Path): JsValue = JsString(o.toString)
    override def reads(json: JsValue): JsResult[Path] = json.validate[String].map(s => Paths.get(s))
  }

  implicit lazy val projectFileFormat: OFormat[ProjectFile] = {
    ((JsPath \ "path").format[Path] ~ (JsPath \ "content")
      .format[ProjectFileContent])(ProjectFile.apply, unlift(ProjectFile.unapply))
  }
}

final case class ProjectFileContent(content: String)

object ProjectFileContent {
  implicit val projectFileContentFormat: OFormat[ProjectFileContent] = Json.format[ProjectFileContent]
}

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

final case class ProjectBuildConfigurationRequest(projectId: ProjectId, projectFile: ProjectFile)

object ProjectBuildConfigurationRequest {
  implicit val projectBuildConfigurationRequestFormat: OFormat[ProjectBuildConfigurationRequest] =
    Json.format[ProjectBuildConfigurationRequest]
}

final case class ProjectUpdateFileRequest(project: Project, projectFile: ProjectFile, version: Option[PipelineVersion])

object ProjectUpdateFileRequest {
  implicit lazy val projectUpdateFileRequestFormat: OFormat[ProjectUpdateFileRequest] =
    ((JsPath \ "project").format[Project] ~ (JsPath \ "projectFile").format[ProjectFile] ~ (JsPath \ "version")
      .formatNullable[PipelineVersion])(
      ProjectUpdateFileRequest.apply,
      unlift(ProjectUpdateFileRequest.unapply)
    )
}

/**
 * Class represents dummy object
 */
final case class DummyResponseBody()
object DummyResponseBody {
  implicit val dummyResponseBodyNonStrictReads = Reads.pure(DummyResponseBody())
  implicit val dummyResponseBodyWrites = OWrites[DummyResponseBody](_ => Json.obj())
}

/**
 * Wrapper type contains success response message from [[cromwell.pipeline.service.SuccessResponseBody]]
 *
 * @param message any response info which signals successful request
 */
final case class SuccessResponseMessage(message: String) extends AnyVal

object SuccessResponseMessage {
  implicit lazy val successResponseMessageFormat: OFormat[SuccessResponseMessage] =
    Json.format[SuccessResponseMessage]
}

/**
 * Wrapper type contains empty response message to use in [[cromwell.pipeline.service.HttpClient]]
 *
 * @param message empty payload message
 */
final case class EmptyPayload(message: String = "")

object EmptyPayload {
  implicit lazy val emptyPayLoadFormat: OFormat[EmptyPayload] = Json.format[EmptyPayload]
}
