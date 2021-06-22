package cromwell.pipeline.datastorage.dto

import java.nio.file.Path
import ProjectFile.pathFormat
import cats.data.Validated
import cromwell.pipeline.model.wrapper.VersionValue
import play.api.libs.functional.syntax.toInvariantFunctorOps
import play.api.libs.json.{ Format, Json, OFormat }

import java.util.UUID

case class ProjectFileConfiguration(path: Path, inputs: List[FileParameter])

object ProjectFileConfiguration {
  implicit val projectFileConfigurationFormat: OFormat[ProjectFileConfiguration] = Json.format
}

final case class ProjectConfigurationVersion(value: VersionValue) extends Ordered[ProjectConfigurationVersion] {
  import VersionValue._

  def name: String = s"v$value"

  private val ordering: Ordering[ProjectConfigurationVersion] = Ordering.by(v => v.value)

  override def compare(that: ProjectConfigurationVersion): Int = ordering.compare(this, that)

  def increaseValue: ProjectConfigurationVersion =
    this.copy(value = increment(this.value))

  override def toString: String = this.name
}

object ProjectConfigurationVersion {
  import VersionValue._

  def defaultVersion: ProjectConfigurationVersion = apply("v1")

  private val pattern = "^v(.+)$".r

  def apply(versionLine: String): ProjectConfigurationVersion =
    versionLine match {
      case pattern(version) =>
        val validationResult = fromString(version).map(ProjectConfigurationVersion.apply)
        validationResult match {
          case Validated.Valid(content)  => content
          case Validated.Invalid(errors) => throw ProjectConfigurationVersionException(errors.toString)
        }
      case _ => throw ProjectConfigurationVersionException(s"Format of version name: 'v(int)', but got: $versionLine")
    }

  final case class ProjectConfigurationVersionException(message: String) extends Exception

  implicit val projectConfigurationVersionFormat: Format[ProjectConfigurationVersion] =
    implicitly[Format[String]].inmap(ProjectConfigurationVersion.apply, _.name)
}

final case class ProjectConfigurationId(value: String)

object ProjectConfigurationId {
  def randomId: ProjectConfigurationId = ProjectConfigurationId(UUID.randomUUID().toString)
  implicit lazy val configurationIdFormat: Format[ProjectConfigurationId] =
    implicitly[Format[String]].inmap(ProjectConfigurationId.apply, _.value)
}

case class ProjectConfiguration(
  id: ProjectConfigurationId,
  projectId: ProjectId,
  active: Boolean,
  projectFileConfigurations: List[ProjectFileConfiguration],
  version: ProjectConfigurationVersion
)

object ProjectConfiguration {
  implicit val projectConfigurationFormat: OFormat[ProjectConfiguration] = Json.format
}
