package cromwell.pipeline.datastorage.dto

import play.api.libs.json.{ Format, JsNull, JsResult, JsSuccess, JsValue, Json, Writes }

final case class WomToolNode(name: String, value: Option[String])

object WomToolNode {
  import OptionFormat._

  implicit object WomToolNodeFormat extends Format[WomToolNode] {
    override def writes(o: WomToolNode): JsValue = Json.obj(o.name -> o.value)

    override def reads(json: JsValue): JsResult[WomToolNode] = {
      val (name, value) = json.as[(String, Option[String])]
      JsSuccess(WomToolNode(name, value))
    }
  }
}

final case class WomToolNodesList(nodes: List[WomToolNode])

object WomToolNodesList {

  implicit object WomToolNodesListFormat extends Format[WomToolNodesList] {
    override def writes(o: WomToolNodesList): JsValue =
      Json.obj(o.nodes.map(node => node.name -> Json.toJsFieldJsValueWrapper(node.value)): _*)

    override def reads(json: JsValue): JsResult[WomToolNodesList] = JsSuccess(
      WomToolNodesList(json.as[List[WomToolNode]])
    )
  }
}

object OptionFormat {
  implicit def optionFormat[T: Format]: Format[Option[T]] =
    new Format[Option[T]] {
      override def reads(json: JsValue): JsResult[Option[T]] =
        json.validateOpt[T]

      override def writes(o: Option[T]): JsValue = o match {
        case Some(t) => implicitly[Writes[T]].writes(t)
        case None    => JsNull
      }
    }
}
