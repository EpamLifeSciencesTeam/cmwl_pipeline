package cromwell.pipeline.utils.json

import cromwell.pipeline.utils.json.AdtJsonFormatter._
import cromwell.pipeline.utils.json.DummyTraitFormatter._
import cromwell.pipeline.utils.json.DummyTraitGen._
import org.scalatest.{ Matchers, WordSpec }
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{ JsSuccess, Json, OFormat }

class AdtJsonFormatterTest extends WordSpec with ScalaCheckDrivenPropertyChecks with Matchers {

  "AdtJsonFormatter" should {

    "write instance" when {
      "this is an instance of a class" in {
        val dummyImplA = DummyImplA("StringField", Some(1))
        noException should be thrownBy Json.toJson(dummyImplA)
      }
      "this is a singleton object" in {
        noException should be thrownBy Json.toJson(DummyImplObj)
      }
    }

    "fail to write instance" when {
      "there is no Writes for this instance" in {
        val brokenFormat: OFormat[DummyTrait] = {
          implicit val dummyImplAFormat: OFormat[DummyImplA] = Json.format
          implicit val dummyImplBFormat: OFormat[DummyImplB] = Json.format
          implicit val dummyImplObjFormat: OFormat[DummyImplObj.type] = objectFormat(DummyImplObj)

          adtFormat("$type")(
            adtCase[DummyImplB]("DummyImplB"),
            adtCase[DummyImplObj.type]("DummyImplObj")
          )
        }

        val dummyImplA: DummyTrait = DummyImplA("StringField", Some(1))
        an[AdtJsonFormatterException] should be thrownBy Json.toJson(dummyImplA)(brokenFormat)

      }
      "instance already contains field with such name" in {
        val brokenFormat: OFormat[DummyTrait] = {
          implicit val dummyImplAFormat: OFormat[DummyImplA] = Json.format
          implicit val dummyImplBFormat: OFormat[DummyImplB] = Json.format
          implicit val dummyImplObjFormat: OFormat[DummyImplObj.type] = objectFormat(DummyImplObj)

          adtFormat("firstAField")(
            adtCase[DummyImplA]("DummyImplA"),
            adtCase[DummyImplB]("DummyImplB"),
            adtCase[DummyImplObj.type]("DummyImplObj")
          )
        }

        val dummyImplA: DummyTrait = DummyImplA("StringField", Some(1))
        an[AdtJsonFormatterException] should be thrownBy Json.toJson(dummyImplA)(brokenFormat)

      }
    }

    "read instance" when {
      "this is an instance of a class" in {
        val json = """{"$type":"DummyImplA","firstAField":"StringField","secondAField":1}"""
        noException should be thrownBy Json.parse(json).as[DummyTrait]
      }
      "this is a singleton object" in {
        val json = """{"$type":"DummyImplObj"}"""
        noException should be thrownBy Json.parse(json).as[DummyTrait]
      }
    }

    "fail to read instance" when {
      "instance is not an object" in {
        val json = """["one", "two"]"""
        an[Exception] should be thrownBy Json.parse(json).as[DummyTrait]
      }

      "instance does not contain typeFieldName key" in {
        val json = """{"firstAField":"StringField","secondAField":1}"""
        an[Exception] should be thrownBy Json.parse(json).as[DummyTrait]
      }

      "instance contain nonexistent typeFieldName" in {
        val json = """{"$type":"DummyImplC","firstAField":"StringField","secondAField":1}"""
        an[Exception] should be thrownBy Json.parse(json).as[DummyTrait]
      }

      "instance contain typeFieldName that is not a string" in {
        val json =
          """{"$type":{"$type":"DummyImplA","firstAField":"StringField","secondAField":1},"firstAField":"StringField","secondAField":1}"""
        an[Exception] should be thrownBy Json.parse(json).as[DummyTrait]
      }
    }

    "read and write instance" when {
      "this is an arbitrary implementation" in {
        forAll(dummyTraitGen, minSuccessful(1024)) { instance =>
          val jsonStr = Json.stringify(Json.toJson(instance))
          val jsResult = Json.fromJson(Json.parse(jsonStr))
          jsResult shouldBe a[JsSuccess[_]]
          val instanceFromJson = jsResult.get
          instanceFromJson shouldBe instance
        }
      }
    }
  }

}
