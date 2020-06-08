package cromwell.pipeline.utils.json

import org.scalacheck.Gen

object DummyTraitGen {

  lazy val dummyTraitGen: Gen[DummyTrait] = Gen.frequency((9, classesGen), (1, objectGen))

  private lazy val classesGen: Gen[DummyTrait] = Gen.oneOf(dummyImplAGen, dummyImplBGen)
  private lazy val objectGen: Gen[DummyTrait] = dummyImplObjGen

  private lazy val stringGen: Gen[String] = Gen.asciiStr.suchThat(_.length < 128)
  private lazy val intGen: Gen[Int] = Gen.choose(-1024, 1024)

  private lazy val dummyImplAGen: Gen[DummyImplA] =
    for {
      firstAField <- stringGen
      secondAField <- Gen.option(intGen)
    } yield DummyImplA(firstAField, secondAField)

  private lazy val dummyImplBGen: Gen[DummyImplB] =
    for {
      firstBField <- intGen
      secondBField <- stringGen
      thirdBField <- Gen.option(stringGen)
    } yield DummyImplB(firstBField, secondBField, thirdBField)

  private lazy val dummyImplObjGen: Gen[DummyImplObj.type] = Gen.const(DummyImplObj)
}
