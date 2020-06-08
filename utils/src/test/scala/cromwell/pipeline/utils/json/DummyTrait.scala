package cromwell.pipeline.utils.json

sealed trait DummyTrait
case class DummyImplA(firstAField: String, secondAField: Option[Int]) extends DummyTrait
case class DummyImplB(firstBField: Int, secondBField: String, thirdBField: Option[String]) extends DummyTrait
case object DummyImplObj extends DummyTrait
