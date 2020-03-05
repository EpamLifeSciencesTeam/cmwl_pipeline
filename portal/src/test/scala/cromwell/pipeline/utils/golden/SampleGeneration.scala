package cromwell.pipeline.utils.golden

import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import play.api.libs.json.JsValue
import scala.util.Try

trait SampleGeneration[A] { self: CodecLaws[A] =>

  def size: Int
  def gen: Gen[A]

  protected lazy val params: Gen.Parameters = Gen.Parameters.default.withSize(size)

  final def getValue(seed: Seed): A = gen.pureApply(params, seed)
  final def getValueFromBase64Seed(seed: String): Try[A] = Seed.fromBase64(seed).map(getValue)

  final def generateRandomGoldenSamples(count: Int): List[(Seed, A, JsValue)] =
    List.fill(count)(count).map { _ =>
      val seed = Seed.random()
      val value = getValue(seed)

      (seed, value, serialize(value))
    }
}
