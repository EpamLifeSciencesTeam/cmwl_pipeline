package cromwell.pipeline.utils.golden

import cats.Applicative
import cats.Eq
import cats.laws.discipline.catsLawsIsEqToProp
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws
import play.api.libs.json.JsResult

import scala.language.higherKinds

trait CodecTests[A] extends Laws {

  def laws: CodecLaws[A]

  def tests(
             implicit
             arbitrary: Arbitrary[A],
             eqA: Eq[JsResult[A]],
             applicative: Applicative[JsResult]
           ): RuleSet =
    new DefaultRuleSet(
      name = "codec tests",
      parent = None,
      "roundTrip" -> Prop.forAll { a: A =>
        laws.codecRoundTrip(a)
      }
    )
}

object CodecTests {
  def apply[A](codecLaws: CodecLaws[A]): CodecTests[A] = new CodecTests[A] {
    val laws: CodecLaws[A] = codecLaws
  }
}