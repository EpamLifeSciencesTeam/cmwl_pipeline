package cromwell.pipeline.datastorage.dto

import cromwell.pipeline.model.wrapper.ProjectSearchFilterId
import cromwell.pipeline.utils.json.AdtJsonFormatter._
import play.api.libs.functional.syntax.{ toFunctionalBuilderOps, toInvariantFunctorOps, unlift }
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{ Json, __, _ }

import java.time.Instant

sealed trait ProjectSearchQuery extends Serializable
case object All extends ProjectSearchQuery
final case class ByName(mode: NameSearchMode, value: String) extends ProjectSearchQuery
final case class ByConfig(mode: ContentSearchMode, value: Boolean) extends ProjectSearchQuery
final case class ByFiles(mode: ContentSearchMode, value: Boolean) extends ProjectSearchQuery
final case class Or(left: ProjectSearchQuery, right: ProjectSearchQuery) extends ProjectSearchQuery
final case class And(left: ProjectSearchQuery, right: ProjectSearchQuery) extends ProjectSearchQuery

object ProjectSearchQuery {

  implicit val projectSearchQueryFormat: OFormat[ProjectSearchQuery] = {

    implicit val allFormat: OFormat[All.type] = objectFormat(All)
    implicit val byNameFormat: OFormat[ByName] = Json.format
    implicit val byConfigFormat: OFormat[ByConfig] = Json.format
    implicit val byFilesFormat: OFormat[ByFiles] = Json.format
    implicit val orFormat: OFormat[Or] = getComplexQueryFormat(Or, Or.unapply)
    implicit val andFormat: OFormat[And] = getComplexQueryFormat(And, And.unapply)

    adtFormat("type")(
      adtCase[All.type]("all"),
      adtCase[ByName]("name"),
      adtCase[ByFiles]("files"),
      adtCase[ByConfig]("configurations"),
      adtCase[Or]("or"),
      adtCase[And]("and")
    )
  }
  private def getComplexQueryFormat[T](
    apply: (ProjectSearchQuery, ProjectSearchQuery) => T,
    unapply: T => Option[(ProjectSearchQuery, ProjectSearchQuery)]
  ): OFormat[T] = OFormat(
    ((__ \ "left").lazyRead(projectSearchQueryFormat) ~
      (__ \ "right").lazyRead(projectSearchQueryFormat))(apply),
    ((__ \ "left").lazyWrite(projectSearchQueryFormat) ~
      (__ \ "right").lazyWrite(projectSearchQueryFormat))(unlift(unapply))
  )
}

sealed trait NameSearchMode
case object FullMatch extends NameSearchMode
case object RegexpMatch extends NameSearchMode

object NameSearchMode {
  implicit lazy val nameSearchModeFormat: Format[NameSearchMode] = {
    implicitly[Format[String]].inmap(NameSearchMode.fromString, NameSearchMode.toString)
  }

  def fromString(s: String): NameSearchMode = s match {
    case "full_match"   => FullMatch
    case "regexp_match" => RegexpMatch
  }

  def toString(mode: NameSearchMode): String = mode match {
    case FullMatch   => "full_match"
    case RegexpMatch => "regexp_match"
  }
}

sealed trait ContentSearchMode
case object Exists extends ContentSearchMode

object ContentSearchMode {
  implicit lazy val contentSearchModeFormat: Format[ContentSearchMode] = {
    implicitly[Format[String]].inmap(ContentSearchMode.fromString, ContentSearchMode.toString)
  }

  def fromString(s: String): ContentSearchMode = s match {
    case "exists" => Exists
  }

  def toString(mode: ContentSearchMode): String = mode match {
    case Exists => "exists"
  }
}

final case class ProjectSearchRequest(filter: ProjectSearchQuery)
object ProjectSearchRequest {
  implicit lazy val projectSearchRequestFormat: OFormat[ProjectSearchRequest] =
    Json.format[ProjectSearchRequest]
}

final case class ProjectSearchResponse(id: ProjectSearchFilterId, data: Seq[Project])
object ProjectSearchResponse {
  implicit lazy val projectSearchResponseFormat: OFormat[ProjectSearchResponse] =
    Json.format[ProjectSearchResponse]
}

final case class ProjectSearchFilter(id: ProjectSearchFilterId, query: ProjectSearchQuery, lastUsedAt: Instant)
