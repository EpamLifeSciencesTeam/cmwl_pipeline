package cromwell.pipeline.datastorage.dto

import cats.data.Validated
import cats.implicits._
import cromwell.pipeline.model.wrapper.{ UserId, VersionValue }
import play.api.libs.functional.syntax._
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._
import java.nio.file.{ Path, Paths }
import slick.lifted.MappedTo

final case class Project(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  active: Boolean,
  repositoryId: RepositoryId,
  version: PipelineVersion,
  visibility: Visibility = Private
)
object Project {
  implicit lazy val projectFormat: OFormat[Project] = Json.format[Project]
}

final case class LocalProject(
  projectId: ProjectId,
  ownerId: UserId,
  name: String,
  active: Boolean,
  visibility: Visibility = Private
) {
  def toProject(repositoryId: RepositoryId, version: PipelineVersion): Project =
    Project(
      projectId = projectId,
      ownerId = ownerId,
      name = name,
      active = active,
      repositoryId = repositoryId,
      visibility = visibility,
      version = version
    )
}

final case class PostProject(name: String)

object PostProject {
  implicit lazy val postProject: OFormat[PostProject] = Json.format[PostProject]
}

final case class ProjectId(value: String) extends MappedTo[String]

object ProjectId {
  implicit lazy val projectIdFormat: Format[ProjectId] = implicitly[Format[String]].inmap(ProjectId.apply, _.value)
}

final case class RepositoryId(value: Int) extends MappedTo[Int]

object RepositoryId {
  implicit lazy val repositoryIdFormat: Format[RepositoryId] =
    implicitly[Format[Int]].inmap(RepositoryId.apply, _.value)
}

final case class ProjectAdditionRequest(name: String)

object ProjectAdditionRequest {
  implicit lazy val projectAdditionFormat: OFormat[ProjectAdditionRequest] = Json.format[ProjectAdditionRequest]
}

final case class ProjectUpdateNameRequest(name: String)

object ProjectUpdateNameRequest {
  implicit val updateRequestFormat: OFormat[ProjectUpdateNameRequest] = Json.format[ProjectUpdateNameRequest]
}

final case class GitLabRepositoryResponse(id: RepositoryId)

object GitLabRepositoryResponse {
  implicit val gitLabRepositoryResponseFormat: OFormat[GitLabRepositoryResponse] = Json.format[GitLabRepositoryResponse]
}

final case class GitLabVersion(commit: Commit, name: PipelineVersion)

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
    this.copy(major = increment(this.major), minor = resetValue, revision = resetValue)

  def increaseMinor: PipelineVersion =
    this.copy(minor = increment(this.minor), revision = resetValue)

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
object Commit {
  implicit val commitFormat: OFormat[Commit] = Json.format[Commit]
}

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

final case class ProjectFileContent(content: String) extends AnyVal

object ProjectFileContent {
  implicit val projectFileContentFormat: Format[ProjectFileContent] = Json.valueFormat[ProjectFileContent]
}

final case class GitLabFileContent(content: String)

object GitLabFileContent {
  implicit lazy val encodingProjectFileContentFormat: OFormat[GitLabFileContent] = Json.format[GitLabFileContent]
}

sealed trait Visibility
case object Private extends Visibility
case object Internal extends Visibility
case object Public extends Visibility

object Visibility {
  implicit lazy val visibilityFormat: Format[Visibility] =
    implicitly[Format[String]].inmap(Visibility.fromString, Visibility.toString)

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

final case class ValidateFileContentRequest(content: ProjectFileContent)

object ValidateFileContentRequest {
  implicit lazy val validateFileContentRequestFormat: OFormat[ValidateFileContentRequest] = Json.format
}

final case class ProjectUpdateFileRequest(
  projectFile: ProjectFile,
  version: Option[PipelineVersion]
)

object ProjectUpdateFileRequest {
  implicit lazy val projectUpdateFileRequestFormat: OFormat[ProjectUpdateFileRequest] =
    ((JsPath \ "projectFile").format[ProjectFile] ~
      (JsPath \ "version").formatNullable[PipelineVersion])(
      ProjectUpdateFileRequest.apply,
      unlift(ProjectUpdateFileRequest.unapply)
    )
}

/**
 * Class represents dummy object
 */
final case class DummyResponseBody()
object DummyResponseBody {
  implicit val dummyResponseBodyNonStrictReads: Reads[DummyResponseBody] = Reads.pure(DummyResponseBody())
  implicit val dummyResponseBodyWrites: OWrites[DummyResponseBody] = OWrites[DummyResponseBody](_ => Json.obj())
}

/**
 * Wrapper type contains success response message
 *
 * @param message any response info which signals successful request
 */
final case class SuccessResponseMessage(message: String) extends AnyVal

object SuccessResponseMessage {
  implicit lazy val successResponseMessageFormat: OFormat[SuccessResponseMessage] =
    Json.format[SuccessResponseMessage] // todo Json.valueFormat ?
}

/**
 * Wrapper type contains success response message
 *
 * @param filePath where the file was stored
 * @param branch where the file was stored
 */
final case class UpdateFiledResponse(filePath: String, branch: String)

object UpdateFiledResponse {
  implicit val config = JsonConfiguration(SnakeCase)
  implicit lazy val updateFiledResponseFormat: OFormat[UpdateFiledResponse] =
    Json.format[UpdateFiledResponse]
}

/**
 * Wrapper type contains empty payload
 * Useful if you want to post something and payload doesn't matter
 *
 * @param message empty payload message
 */
final case class EmptyPayload(message: String = "")

object EmptyPayload {
  implicit lazy val emptyPayLoadFormat: OFormat[EmptyPayload] = Json.format[EmptyPayload]
}
