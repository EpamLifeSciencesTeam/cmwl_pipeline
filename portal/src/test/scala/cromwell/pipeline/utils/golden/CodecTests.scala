package cromwell.pipeline.utils.golden

import cats.Eq
import cats.instances.string._
import cats.laws.discipline.catsLawsIsEqToProp
import cats.laws.IsEq
import org.scalacheck.{ Arbitrary, Prop }
import org.typelevel.discipline.Laws
import play.api.libs.json.Format
import scala.reflect.runtime.universe.TypeTag
import scala.util.{ Failure, Success, Try }

trait CodecTests[A] extends Laws {

  def laws: CodecLaws[A]

  def tests(
    implicit
    arbitrary: Arbitrary[A],
    eqA: Eq[A]
  ): RuleSet =
    new DefaultRuleSet(
      name = "codec tests",
      parent = None,
      "encoding test" -> tryListToProp(laws.encoding),
      "decoding test" -> tryListToProp(laws.decoding)
    )

  private def tryListToProp[B: Eq](result: Try[List[IsEq[B]]]): Prop = result match {
    case Failure(error)      => Prop.exception(error)
    case Success(equalities) => Prop.all(equalities.map(catsLawsIsEqToProp(_)): _*)
  }
}

object CodecTests {
  def apply[A: Format: Arbitrary](codecLaws: CodecLaws[A]): CodecTests[A] = new CodecTests[A] {
    val laws: CodecLaws[A] = codecLaws
  }

  def apply[A: Format: Arbitrary: TypeTag]: CodecTests[A] = apply[A](ResourceFileCodecLaws[A])
}
