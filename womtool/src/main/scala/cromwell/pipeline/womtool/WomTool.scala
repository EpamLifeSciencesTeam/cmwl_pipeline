package cromwell.pipeline.womtool

import cats.data.NonEmptyList
import com.typesafe.config.ConfigFactory
import cromwell.languages.util.ImportResolver.ImportResolver
import languages.cwl.CwlV1_0LanguageFactory
import languages.wdl.biscayne.WdlBiscayneLanguageFactory
import languages.wdl.draft2.WdlDraft2LanguageFactory
import languages.wdl.draft3.WdlDraft3LanguageFactory
import spray.json._
import spray.json.DefaultJsonProtocol._
import wom.executable.WomBundle
import wom.expression.WomExpression
import wom.graph.{
  ExternalGraphInputNode,
  OptionalGraphInputNode,
  OptionalGraphInputNodeWithDefault,
  RequiredGraphInputNode
}
import wom.types.{ WomCompositeType, WomOptionalType, WomType }
import womtool.input.WomGraphWithResolvedImports

class WomTool(val importResolvers: List[ImportResolver]) extends WomToolAPI {

  private val defaultLanguageFactory = new WdlDraft2LanguageFactory(ConfigFactory.empty())
  private val languageFactories = List(
    new WdlDraft3LanguageFactory(ConfigFactory.empty()),
    new WdlBiscayneLanguageFactory(ConfigFactory.empty()),
    new CwlV1_0LanguageFactory(ConfigFactory.empty())
  )

  def validate(content: String): Either[NonEmptyList[String], WomBundle] = read(content)

  def inputs(content: String): Either[NonEmptyList[String], String] =
    for {
      bundle <- read(content)
      callable <- bundle.toExecutableCallable
      wdl = WomGraphWithResolvedImports(callable.graph, bundle.resolvedImportRecords)
    } yield wdl.graph.externalInputNodes.toJson(inputNodeWriter).prettyPrint + System.lineSeparator

  private def inputNodeWriter: JsonWriter[Set[ExternalGraphInputNode]] = set => {
    val valueMap: Seq[(String, JsValue)] = set.toList.collect {
      case RequiredGraphInputNode(_, womType, nameInInputSet, _) => nameInInputSet -> womTypeToJson(womType, None)
      case OptionalGraphInputNode(_, womOptionalType, nameInInputSet, _) =>
        nameInInputSet -> womTypeToJson(womOptionalType, None)
      case OptionalGraphInputNodeWithDefault(_, womType, default, nameInInputSet, _) =>
        nameInInputSet -> womTypeToJson(womType, Option(default))
    }
    valueMap.toMap.toJson
  }

  private def womTypeToJson(womType: WomType, default: Option[WomExpression]): JsValue = (womType, default) match {
    case (WomCompositeType(typeMap, _), _) =>
      JsObject(typeMap.map { case (name, wt) => name -> womTypeToJson(wt, None) })
    case (_, Some(d))            => JsString(s"${womType.stableName} (optional, default = ${d.sourceString})")
    case (_: WomOptionalType, _) => JsString(s"${womType.stableName} (optional)")
    case (_, _)                  => JsString(s"${womType.stableName}")
  }

  private def read(content: String): Either[NonEmptyList[String], WomBundle] = {
    val languageFactory = languageFactories.find(_.looksParsable(content)).getOrElse(defaultLanguageFactory)
    languageFactory.getWomBundle(content, None, "{}", importResolvers, List(languageFactory))
  }
}
