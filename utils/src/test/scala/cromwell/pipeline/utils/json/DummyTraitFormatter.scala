package cromwell.pipeline.utils.json

import cromwell.pipeline.utils.json.AdtJsonFormatter._
import play.api.libs.json.{ Json, OFormat }

object DummyTraitFormatter {

  implicit val dummyTraitFormat: OFormat[DummyTrait] = {
    implicit val dummyImplAFormat: OFormat[DummyImplA] = Json.format[DummyImplA]
    implicit val dummyImplBFormat: OFormat[DummyImplB] = Json.format[DummyImplB]
    implicit val dummyImplObjFormat: OFormat[DummyImplObj.type] = objectFormat(DummyImplObj)

    adtFormat("$type")(
      adtCase[DummyImplA]("DummyImplA"),
      adtCase[DummyImplB]("DummyImplB"),
      adtCase[DummyImplObj.type]("DummyImplObj")
    )
  }

}
